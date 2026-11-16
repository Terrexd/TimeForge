package es.timeforge.integration;

import com.fasterxml.jackson.databind.JsonNode;
import es.timeforge.model.Domain.MediaType;
import es.timeforge.service.BusinessException;
import es.timeforge.controller.Inputs.WorkInput;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/** Adaptación pura de RAWG/Jikan al dominio, independiente de HTTP y persistencia. */
public final class CatalogMapper {
    private static final Pattern HOURS=Pattern.compile("(\\d+)\\s*hr");
    private static final Pattern MINUTES=Pattern.compile("(\\d+)\\s*min");
    private CatalogMapper() {}
    public record ImportedWork(WorkInput input,String coverUrl) {}

    public static ImportedWork map(String source,JsonNode response) {
        boolean game=source.equals("RAWG");
        boolean manga=source.equals("JIKAN_MANGA");
        JsonNode data=game?response:response.path("data");
        String title=data.path(game?"name":"title").asText();
        BusinessException.require(!title.isBlank(),"La ficha externa no contiene un título válido");
        String synopsis=data.path(game?"description_raw":"synopsis").asText("");
        StringJoiner genres=new StringJoiner(", ");
        for(var genre:data.path("genres")) genres.add(genre.path("name").asText());
        int total=game?data.path("playtime").asInt(1):data.path(manga?"chapters":"episodes").asInt(1);
        int duration=game?60:manga?10:episodeMinutes(data.path("duration").asText());
        String cover=game?data.path("background_image").asText():data.path("images").path("jpg").path("image_url").asText();
        var input=new WorkInput(limit(title,150),game?MediaType.GAME:manga?MediaType.MANGA:MediaType.ANIME,
                limit(synopsis,8000),limit(genres.toString(),250),Math.max(1,total),duration,false);
        return new ImportedWork(input,cover);
    }

    public static ImportedWork mapAniList(String source,JsonNode media) {
        boolean manga=source.equals("JIKAN_MANGA");
        String title=media.path("title").path("english").asText("");
        if(title.isBlank()) title=media.path("title").path("romaji").asText("");
        if(title.isBlank()) title=media.path("title").path("native").asText("");
        BusinessException.require(!title.isBlank(),"La ficha externa no contiene un título válido");
        String synopsis=media.path("description").asText("");
        StringJoiner genres=new StringJoiner(", ");
        for(var g:media.path("genres")) genres.add(g.asText());
        int total=media.path(manga?"chapters":"episodes").asInt(1);
        int duration=manga?10:24;
        String cover=media.path("coverImage").path("large").asText("");
        var input=new WorkInput(limit(title,150),manga?MediaType.MANGA:MediaType.ANIME,
                limit(synopsis,8000),limit(genres.toString(),250),Math.max(1,total),duration,false);
        return new ImportedWork(input,cover);
    }

    public static int episodeMinutes(String value) {
        var hours=HOURS.matcher(value);
        var minutes=MINUTES.matcher(value);
        int result=0;
        if(hours.find()) result+=Integer.parseInt(hours.group(1))*60;
        if(minutes.find()) result+=Integer.parseInt(minutes.group(1));
        return result>0?result:24;
    }

    private static String limit(String value,int length) {return value.substring(0,Math.min(length,value.length()));}
}
