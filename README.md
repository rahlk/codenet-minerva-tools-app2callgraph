# Konveyor DGI code2graph
```
   _____            _        ___    _____                     _     
  / ____|          | |      |__ \  / ____|                   | |    
 | |      ___    __| |  ___    ) || |  __  _ __  __ _  _ __  | |__  
 | |     / _ \  / _` | / _ \  / / | | |_ || '__|/ _` || '_ \ | '_ \ 
 | |____| (_) || (_| ||  __/ / /_ | |__| || |  | (_| || |_) || | | |
  \_____|\___/  \__,_| \___||____| \_____||_|   \__,_|| .__/ |_| |_|
                                                      | |           
                                                      |_|           
```

Native WALA implementation of code2graph for [konveyor/data-gravity-insights](https://github.com/konveyor/tackle-data-gravity-insights)

## Prequisits 

- Java 11
- Optionally, graphviz (for visualization)

## Usage

```man
./code2graph [-h] [-i <arg>] [-o <arg>] [-q]
Convert java binary (*.jar, *.ear, *.war) to a neo4j graph.

 -h,--help            Print this help message.
 -d,--outdir <arg>    Destination (directory) to save the output graph.
 -i,--input <arg>     Path to the input jar(s). For multiple JARs,
                      separate them with ':'. E.g., file1.jar:file2.jar,
                      etc.
 -o,--outfile <arg>   Destination (filename) to save the output graph (as
                      graphml/dot/json).
 -q,--quiet           Don't print logs to console.
```

### Examples

There are some sample binaries in `etc/demo`. Some additional example usages include:

1. Process multiple JAR files
   ```
   ./code2graph --input=etc/demo/jar/daytrader-ee7-ejb.jar:etc/demo/jar/daytrader-ee7-web.jar --outdir=etc/demo/output/ --outfile=daytrader.json
   ```

2. Process EAR file(s)
   ```
   ./code2graph --input=etc/demo/ear/daytrader-ee7.ear --outdir=etc/demo/output/ --outfile=daytrader.json
   ```

3. Process WAR file(s)
   ```
   ./code2graph --input=etc/demo/war/daytrader-ee7-web.war --outdir=etc/demo/output/ --outfile=daytrader.json
   ```
