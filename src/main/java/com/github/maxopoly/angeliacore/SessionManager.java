package com.github.maxopoly.angeliacore;

import com.github.maxopoly.angeliacore.connection.login.AuthenticationHandler;
import com.github.maxopoly.angeliacore.exceptions.OutDatedAuthException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class SessionManager {

	private File saveFile;
	private File defaultSaveFile = new File("angeliaData/tokens.json");
	private Logger logger;
	private String clientToken;
	private boolean legacyFormat;

	public SessionManager(Logger logger, File saveFile) {
		this.logger = logger;
		this.saveFile = saveFile == null ? defaultSaveFile : saveFile;
		//do once to load client token
		getAvailableAccounts();
		if (clientToken == null) {
			clientToken = UUID.randomUUID().toString();
		}
	}

	public AuthenticationHandler getAccountByEmail(String email) {
		for(AuthenticationHandler auth : getAvailableAccounts()) {
			if(auth.getEmail().equalsIgnoreCase(email)) {
				return auth;
			}
		}
		return null;
	}

	public synchronized AuthenticationHandler authNewAccount(String userName, String password) {
		AuthenticationHandler authHandler;
		try {
			authHandler = new AuthenticationHandler(this, userName, password, clientToken, logger);
		} catch (IOException e) {
			logger.error("Failed to auth account " + userName, e);
			return null;
		}
		return authHandler;
	}

	public List<AuthenticationHandler> getAvailableAccounts() {
		return reloadFileContent();
	}

	private JSONObject loadAuthJson(File file) {
		if (!saveFile.exists()) {
			logger.info("Could not load auth token file because it didnt exist?");
			return null;
		}
		String content;
		try {
			content = new String(Files.readAllBytes(file.toPath()));
		} catch (IOException e) {
			logger.error("Failed to read profile file", e);
			return null;
		}
		return new JSONObject(content);
	}

	private List<AuthenticationHandler> reloadFileContent() {
		List<AuthenticationHandler> auths = new LinkedList<AuthenticationHandler>();
		JSONObject json = loadAuthJson(saveFile);
		if (json == null) {
			return auths;
		}
		clientToken = json.optString("clientToken", UUID.randomUUID().toString());
		JSONObject authSection = json.optJSONObject("authenticationDatabase");
		if (authSection == null) {
			logger.info("No auth section found in file");
			return auths;
		}
		JSONArray names = authSection.names();
		if (names == null) {
			logger.info("Auth section in file was empty");
			return auths;
		}
		auths.clear();
		for (int i = 0; i < names.length(); i++) {
			String identifier = names.getString(i);
			JSONObject authObj = authSection.getJSONObject(identifier);
			String name = authObj.optString("displayName", null);
			String accessToken = authObj.getString("accessToken");
			String email = authObj.getString("username");
			legacyFormat = name != null;
			AuthenticationHandler auth;
			String userID;
			String uuid;
			if (legacyFormat) {
				userID = authObj.getString("userid");
				uuid = names.getString(i);
			} else {
				userID = names.getString(i);
				JSONObject profiles = authObj.optJSONObject("profiles");
				if (profiles == null) {
					logger.info("Token file is missing profile information for account " + email);
					continue;
				}
				JSONArray profileNames = profiles.names();
				if (profileNames == null) {
					logger.info("Token file is missing profile entries for account " + email);
					continue;
				}
				uuid = "";
				for (int k = 0; k < profileNames.length(); k++) {
					JSONObject section = profiles.getJSONObject(profileNames.getString(k));
					uuid = profileNames.getString(k);
					name = section.getString("displayName");
				}
			}
			try {
				auth = new AuthenticationHandler(this, name, accessToken, email, uuid, userID, clientToken, logger);
			} catch (IOException e) {
				logger.warn("Failed to load auth for  " + name);
				continue;
			}
			catch (OutDatedAuthException e) {
				logger.info("Auth for " + name + " was outdated, it was ignored");
				continue;
			}
			auths.add(auth);
		}
		return auths;
	}

	public synchronized void deleteAuth(AuthenticationHandler auth) {
		logger.info("Deleting access tokens for " + auth.getPlayerName() + " from save file");
		JSONObject json = loadAuthJson(saveFile);
		if (json == null) {
			return;
		}
		json.put("clientToken", clientToken);
		JSONObject authSection = json.optJSONObject("authenticationDatabase");
		if (authSection == null) {
			return;
		}
		String identifierString = legacyFormat ? auth.getPlayerUUID() : auth.getUserID();
		authSection.remove(identifierString);
		saveJSON(saveFile, json);
	}

	public synchronized void updateAuth(AuthenticationHandler auth) {
		logger.info("Updating access tokens for " + auth.getPlayerName() + " in save file");
		JSONObject json = loadAuthJson(saveFile);
		if (json == null) {
			json = new JSONObject();
		}
		if (!saveFile.exists()) {
			logger.info("Token save file did not exst, creating it...");
			saveFile.mkdirs();
		}
		json.put("clientToken", clientToken);
		JSONObject authSection = json.optJSONObject("authenticationDatabase");
		if (authSection == null) {
			logger.info("No auth section found in auth token file, creating it");
			authSection = new JSONObject();
			json.put("authenticationDatabase", authSection);
		}
		JSONObject authObj;
		String identifierString = legacyFormat ? auth.getPlayerUUID() : auth.getUserID();
		authObj = authSection.optJSONObject(identifierString);
		if (authObj == null) {
			authObj = new JSONObject();
			authSection.put(identifierString, authObj);
		}
		if (legacyFormat) {
			authObj.put("displayName", auth.getPlayerName());
			authObj.put("userid", auth.getUserID());
			// quick way to add the dashes into the uuid
			authObj.put("uuid", auth.getPlayerUUID()
					.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
		} else {
			JSONObject profilesObject = new JSONObject();
			authObj.put("profiles", profilesObject);
			JSONObject uuidObject = new JSONObject();
			profilesObject.put(auth.getPlayerUUID(), uuidObject);
			uuidObject.put("displayName", auth.getPlayerName());
		}
		authObj.put("accessToken", auth.getAccessToken());
		authObj.put("username", auth.getEmail());
		saveJSON(saveFile, json);
	}

	private void saveJSON(File file, JSONObject json) {
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(json.toString());
		} catch (IOException e) {
			logger.error("Failed to save auth tokens to save file", e);
			return;
		}
	}
}
