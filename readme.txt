Playing with cassandra API.


1. Gathers rates from public BTC api https://blockchain.info/ticker
2. moves it into cassandra 


how to use

1.  tune cassandra to be accessible from net or run this program on the same node where cassandra controller node is installed
2. tune ini file
3. run it: java -jar cass_test-1.0-SNAPSHOT.jar -o <ini file name>


Example ini file:

cassandra_ip = 192.168.1.79
cassandra_port = 9042
btc_rates_url = https://blockchain.info/ticker

handlers= java.util.logging.FileHandler
java.util.logging.FileHandler.pattern = C:\\PROJECTS\\JAVA\\FILES\\logs\\1.log
java.util.logging.FileHandler.limit = 100000
java.util.logging.FileHandler.count = 7
java.util.logging.FileHandler.formatter = java.util.logging.SimpleFormatter
java.util.logging.FileHandler.level = ALL
