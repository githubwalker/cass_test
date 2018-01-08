package com.alprojects.castest;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrew on 06.01.2018.
 */
public class QuotasPump implements Closeable
{
    private String cas_serverIp;
    private int cas_port;
    private String btcRatesUrl;

    private Cluster cassandra_cluster = null;
    private Session cassandra_session = null;
    private UrlJsonLoader urlJsonLoader = null;
    private Logger log = null;

    QuotasPump(
            String cas_serverIp,
            int cas_port,
            String btcRatesUrl
    )
    {
        this.cas_serverIp = cas_serverIp;
        this.cas_port = cas_port;
        this.btcRatesUrl = btcRatesUrl;
        log = Logger.getLogger(QuotasPump.class.getName());
    }

    void open() throws IOException
    {
        cassandra_cluster = Cluster.builder().addContactPoint(cas_serverIp).withPort(cas_port).build();
        cassandra_session = cassandra_cluster.connect();
        urlJsonLoader = new UrlJsonLoader( btcRatesUrl );
    }

    @Override
    public void close() throws IOException
    {
        if (cassandra_cluster != null)
        {
            cassandra_cluster.close();
            cassandra_cluster = null;
        }

        if (cassandra_session != null)
        {
            cassandra_session.close();
            cassandra_session = null;
        }
    }

    private void pump_helper() throws IOException
    {
        Instant instantNow = Instant.now();
        LocalDateTime utcTime = LocalDateTime.ofInstant(instantNow, ZoneId.of("UTC"));
        String strDate = String.format("%04d%02d%02d", utcTime.getYear(), utcTime.getMonth().getValue(), utcTime.getDayOfMonth());
        int dayMinute = utcTime.getHour() * 60 + utcTime.getMinute();

        Map<String,Double> rates = new HashMap<String,Double>();

        JSONObject json = new JSONObject(urlJsonLoader.loadJson());
        for ( String key: json.keySet())
        {
            JSONObject currencyRateObject = json.getJSONObject(key);
            double lastRate4currency = currencyRateObject.getDouble("last");
            String ticker = "BTC" + key;
            rates.put( ticker, lastRate4currency );
            log.log(Level.INFO, String.format("for BTC%s last rate is :%.2f", key, lastRate4currency));
        }

        // https://groups.google.com/a/lists.datastax.com/forum/#!topic/java-driver-user/JNCOk53GXKs
        for( String key : rates.keySet() )
        {
            PreparedStatement preparedStatement = cassandra_session.prepare(
                    "insert into quotes.rates (ticker, day, day_minute, time, rate) values (:ticker, :day_str, :day_time_int, :tm, :rate)");
            BoundStatement bs = preparedStatement.bind();
            bs.setString("ticker", key);
            bs.setString( "day_str", strDate);
            bs.setInt("day_time_int", dayMinute);
            bs.setTimestamp("tm", Date.from(instantNow));
            bs.setDouble("rate", rates.get(key));

            cassandra_session.execute(bs);
        }
    }

    void pump() throws IOException
    {
        try
        {
            pump_helper();
        }
        catch(Exception ex)
        {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            log.log( Level.SEVERE, "Failed pump quotas" );
            log.log( Level.SEVERE, errors.toString());
        }
    }
}

