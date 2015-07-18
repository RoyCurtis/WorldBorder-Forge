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
 */
public class NameFetcher
{
    private static final String     URL    = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final JsonParser PARSER = new JsonParser();

    public static String[] fetch(UUID[] uuids) throws Exception
    {
        String[] names = new String[uuids.length];

        for (int i = 0; i < uuids.length; i++)
        {
            // TODO: Optimize, and close connection when done
            // TODO: Use server built-in usercache
            String        url      = URL + uuids[i].toString().replace("-", "");
            URLConnection conn     = new URL(url).openConnection();
            Reader        reader   = new InputStreamReader( conn.getInputStream() );
            JsonObject    response = (JsonObject) PARSER.parse(reader);

            String name = response.get("name").getAsString();

            if ( response.has("errorMessage") )
            {
                String error = response.get("errorMessage").getAsString();
                throw new IllegalStateException(error);
            }
            else
                names[i] = (name != null) ? name : "<unknown>";
        }

        return names;
    }
}
