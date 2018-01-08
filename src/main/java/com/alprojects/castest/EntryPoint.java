package com.alprojects.castest;

import org.apache.commons.cli.*;

import java.io.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


/**
 * Created by andrew on 12.11.2017.
 */

// http://www.nurkiewicz.com/2014/05/interruptedexception-and-interrupting.html
// http://www.nurkiewicz.com/2015/03/completablefuture-cant-be-interrupted.html



public class EntryPoint
{
    // static String cas_serverIp = "192.168.1.35";
    // static int cas_port = 9042;
    // static String btcRatesUrl = "https://blockchain.info/ticker";

    public static void printHelp(Options opts)
    {
        HelpFormatter hlp = new HelpFormatter();
        hlp.printHelp("cmdname", opts);
    }

    private static class ProgramOptions
    {
        public String optionFile;
        public String cas_serverIp;
        public int cas_port;
        public String btcRatesUrl;

        public ProgramOptions(String optionFile, String cas_serverIp, int cas_port, String btcRatesUrl)
        {
            this.optionFile = optionFile;
            this.cas_serverIp = cas_serverIp;
            this.cas_port = cas_port;
            this.btcRatesUrl = btcRatesUrl;
        }
    }

    private static ProgramOptions parseOptions(String[] args) throws IOException
    {
        Options opts = new Options()
                .addOption(Option.builder("o").longOpt("options-file-name").desc("options file name").required().hasArg().build())
                .addOption(Option.builder("h").longOpt("help").desc("produce help").required(false).hasArg(false).build())
                ;

        CommandLineParser cmdParser = new DefaultParser();
        CommandLine cmdLine = null;

        try
        {
            cmdLine = cmdParser.parse(opts, args);
            if ( cmdLine.hasOption("h") )
            {
                printHelp(opts);
                System.exit(1);
            }
        }
        catch(Exception ex)
        {
            printHelp(opts);
            System.exit(1);
        }

        String optionFileName = cmdLine.getOptionValue("o");

        Properties props = new Properties();
        props.load(new FileInputStream(new File(optionFileName)));

        return new ProgramOptions(
                optionFileName,
                props.getProperty("cassandra_ip"),
                Integer.valueOf(props.getProperty("cassandra_port")),
                props.getProperty("btc_rates_url")
        );
    }

    static private void setupLogger(String optsFile) throws IOException
    {
        try
        {
            LogManager.getLogManager().readConfiguration(
                    new FileInputStream(new File(optsFile)));
        }
        catch(Exception ex)
        {
            System.err.println( "Failed to setup logger" );
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            System.err.println(errors.toString());
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, ParseException
    {
        ProgramOptions po = parseOptions(args);
        setupLogger(po.optionFile);

        Logger log = Logger.getLogger(EntryPoint.class.getName());
        log.info("Starting Quotas pump");

        final QuotasPumpWorker worker = new QuotasPumpWorker(po.cas_serverIp, po.cas_port, po.btcRatesUrl);

        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                worker.interrupt();
                System.out.println("Cancellation signal caught");
            }
        }
        );

        worker.start();
        worker.join();

        System.exit(0);
    }
}

// left to do:
// 1. logging
// https://habrahabr.ru/post/130195/
//
// 2. run as service under linux

