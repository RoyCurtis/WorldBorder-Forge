package com.wimbli.WorldBorder.UUID;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

/*
 * code by evilmidget38
 * from http://forums.bukkit.org/threads/player-name-uuid-fetcher.250926/
 * slightly modified to fix name case mismatches for single name lookup
 */
public class UUIDFetcher
{
    private static final String     URL    = "https://api.mojang.com/users/profiles/minecraft/";
    private static final JsonParser PARSER = new JsonParser();

    public static UUID fetch(String name) throws Exception
    {
        // TODO: Handle nulls
        String        url      = URL + name;
        URLConnection conn     = new URL(url).openConnection();
        Reader        reader   = new InputStreamReader( conn.getInputStream() );
        JsonObject    response = (JsonObject) PARSER.parse(reader);

        if ( response.has("errorMessage") )
        {
            String error = response.get("errorMessage").getAsString();
            throw new IllegalStateException(error);
        }
        else
            return getUUID( response.get("id").getAsString() );
    }

    private static UUID getUUID(String id)
    {
        return UUID.fromString(
            id.substring(0, 8) + "-"
            + id.substring(8, 12) + "-"
            + id.substring(12, 16) + "-"
            + id.substring(16, 20) + "-"
            + id.substring(20, 32)
        );
    }
}
