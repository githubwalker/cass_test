package com.alprojects.castest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.stream.Collectors;

/**
 * Created by andrew on 05.01.2018.
 */
public class UrlJsonLoader
{
    // private String url;
    private URL objUrl;

    public UrlJsonLoader(String url) throws IOException
    {
        this.objUrl = new URL(url);
    }

    public String loadJson() throws IOException
    {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(objUrl.openStream())))
        {
            return in.lines().collect(Collectors.joining("\r\n"));
        }
    }
}

