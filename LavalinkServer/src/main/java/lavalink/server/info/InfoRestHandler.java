package lavalink.server.info;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by napster on 08.03.19.
 */
@RestController
public class InfoRestHandler {

    private final AppInfo appInfo;
    private static final Logger log = LoggerFactory.getLogger(InfoRestHandler.class);

    public InfoRestHandler(AppInfo appInfo) {
        this.appInfo = appInfo;
    }

    @GetMapping("/version")
    public String version() {
        return appInfo.getVersionBuild();
    }

    private void log(HttpServletRequest request) {
        String path = request.getServletPath();
        log.info("GET " + path);
    }

    @GetMapping("/metadata", produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> getMetaData(HttpServletRequest request) {

        log(request);

        return new ResponseEntity<>(metaDataLoader().toString(), HttpStatus.OK);
    }

    private JSONObject metaDataLoader() {
        GitRepoState gitRepoState = new GitRepoState();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss z").withZone(ZoneId.of("UTC"));
        String buildTime = dtf.format(Instant.ofEpochMilli(appInfo.getBuildTime()));
        String commitTime = dtf.format(Instant.ofEpochMilli(gitRepoState.getCommitTime() * 1000));

        String version = appInfo.getVersion().startsWith("@") ? "Unknown" : appInfo.getVersion();
        String buildNumber = appInfo.getBuildNumber().startsWith("@") ? "Unofficial" : appInfo.getBuildNumber();

        JSONObject json = new JSONObject();

        json.put("version", version);
        json.put("build", buildNumber);
        json.put("buildTime", buildTime);
        json.put("branch", gitRepoState.getBranch());
        json.put("commit", gitRepoState.getCommitIdAbbrev());
        json.put("commitTime", commitTime);
        json.put("jvm", System.getProperty("java.version"));
        json.put("lavaplayer", PlayerLibrary.VERSION);

        if (!gitRepoState.isLoaded()) {
            JSONObject exception = new JSONObject();
            exception.put("message", "Failed parsing info, can not generate metadata, not compiled with git.");
            exception.put("severity", "ERROR");

            json.put("exception", exception);
            log.error("Failed parsing info, Not a git repo.");
        }

        return json;
    }

}
