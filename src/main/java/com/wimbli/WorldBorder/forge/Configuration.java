package com.wimbli.WorldBorder.forge;

import java.io.File;
import java.util.Set;

/**
 * More convenient and compatible version of the Forge configuration class
 */
public class Configuration extends net.minecraftforge.common.config.Configuration
{
    private static final String GENERAL = "General";

    public Configuration(File file)
    {
        super(file);
    }

    public void removeCategory(String category)
    {
        removeCategory(getCategory(category));
    }

    public void clear()
    {
        Set<String> categories = getCategoryNames();

        for(String category : categories)
            removeCategory( getCategory(category) );
    }

    public String getString(String key, String defValue)
    {
        return getString(key, GENERAL, defValue, "");
    }

    public boolean getBoolean(String key, boolean defValue)
    {
        return getBoolean(key, GENERAL, defValue, "");
    }

    public float getFloat(String key, float defValue)
    {
        return getFloat(key, GENERAL, defValue, 0, Float.MAX_VALUE, "");
    }

    public int getInt(String key, int defValue)
    {
        return getInt(key, GENERAL, defValue, Integer.MIN_VALUE, Integer.MAX_VALUE, "");
    }

    public String[] getStringList(String key)
    {
        return getStringList(key, GENERAL, new String[0], "");
    }

    public void set(String key, boolean value)
    {
        getCategory(GENERAL).get(key).set(value);
    }

    public void set(String key, String value)
    {
        getCategory(GENERAL).get(key).set(value);
    }

    public void set(String key, int value)
    {
        getCategory(GENERAL).get(key).set(value);
    }

    public void set(String key, float value)
    {
        getCategory(GENERAL).get(key).set(value);
    }

    public void set(String key, String[] values)
    {
        getCategory(GENERAL).get(key).set(values);
    }
}
