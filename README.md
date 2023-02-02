# Minerva Application to Callgraph
```
   █████████                        ████████    █████████            ████  ████                                         █████     
  ███░░░░░███                      ███░░░░███  ███░░░░░███          ░░███ ░░███                                        ░░███      
 ░███    ░███  ████████  ████████ ░░░    ░███ ███     ░░░   ██████   ░███  ░███   ███████ ████████   ██████   ████████  ░███████  
 ░███████████ ░░███░░███░░███░░███   ███████ ░███          ░░░░░███  ░███  ░███  ███░░███░░███░░███ ░░░░░███ ░░███░░███ ░███░░███ 
 ░███░░░░░███  ░███ ░███ ░███ ░███  ███░░░░  ░███           ███████  ░███  ░███ ░███ ░███ ░███ ░░░   ███████  ░███ ░███ ░███ ░███ 
 ░███    ░███  ░███ ░███ ░███ ░███ ███      █░░███     ███ ███░░███  ░███  ░███ ░███ ░███ ░███      ███░░███  ░███ ░███ ░███ ░███ 
 █████   █████ ░███████  ░███████ ░██████████ ░░█████████ ░░████████ █████ █████░░███████ █████    ░░████████ ░███████  ████ █████
░░░░░   ░░░░░  ░███░░░   ░███░░░  ░░░░░░░░░░   ░░░░░░░░░   ░░░░░░░░ ░░░░░ ░░░░░  ░░░░░███░░░░░      ░░░░░░░░  ░███░░░  ░░░░ ░░░░░ 
               ░███      ░███                                                    ███ ░███                     ░███                
               █████     █████                                                  ░░██████                      █████               
              ░░░░░     ░░░░░                                                    ░░░░░░                      ░░░░░                
                                                                                                         
```

Convert a java application to a callgraph.

## Prequisits 

- Java 11

## Usage

```man
./app2callgraph [-h] [-i <arg>] [-o <arg>] [-q]
Convert java binary (*.jar, *.ear, *.war) to a neo4j graph.

 -h,--help           Print this help message.
 -i,--input <arg>    Path to the input jar(s). For multiple JARs, separate
                     them with ':'. E.g., file1.jar:file2.jar, etc.
 -o,--output <arg>   Destination (directory) to save the output graphs.
 -q,--quiet          Don't print logs to console.
```

### Examples

There are some sample binaries in `etc/demo`. Some additional example usages include:

1. Process multiple JAR files
   ```
   ./app2callgraph --input=etc/demo/jar/daytrader-ee7-ejb.jar:etc/demo/jar/daytrader-ee7-web.jar --output=etc/demo
   ```

2. Process EAR file(s)
   ```
   ./app2callgraph --input=etc/demo/ear/daytrader-ee7.ear --output=etc/demo
   ```

3. Process WAR file(s)
   ```
   ./app2callgraph --input=etc/demo/war/daytrader-ee7-web.war --output=etc/demo
   ```
