package com.alprojects.castest;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrew on 07.01.2018.
 */
public class QuotasPumpWorker extends Thread
{
    private String cas_serverIp;
    private int cas_port;
    private String btcRatesUrl;
    private Logger log;


    QuotasPumpWorker(String cas_serverIp, int cas_port, String btcRatesUrl)
    {
        this.cas_serverIp = cas_serverIp;
        this.cas_port = cas_port;
        this.btcRatesUrl = btcRatesUrl;
        this.log = Logger.getLogger(QuotasPumpWorker.class.getName());
    }

    private void logWithException(Exception ex, String message)
    {
        StringWriter errors = new StringWriter();
        ex.printStackTrace(new PrintWriter(errors));
        log.log( Level.SEVERE, message );
        log.log( Level.SEVERE, errors.toString());
    }

    @Override
    public void run()
    {
        try (QuotasPump qp = new QuotasPump(cas_serverIp, cas_port, btcRatesUrl))
        {
            qp.open();
            while (!Thread.currentThread().isInterrupted()) // !Thread.interrupted()
            {
                try
                {
                    qp.pump();
                    Thread.sleep(1000);
                    log.info("Thread still working");
                }
                catch (InterruptedException ex)
                {
                    logWithException(ex, "InterruptedException caught");
                    break;
                }
                catch (Exception ex)
                {
                    logWithException(ex, "Something wrong happened during pupm process");
                    break;
                }
            }
        } catch (Exception e)
        {
            logWithException(e, "Failed to initialize QuotasPump");
        }

        log.info("Leaving working thread");
    }
}
