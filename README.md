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
- Java 8 (or above)

## Usage

```man
./code2graph [-h] [-i <arg>] [-o <arg>] [-q]
Convert java binary (*.jar, *.ear, *.war) to a neo4j graph.

 -h,--help           Print this help message.
 -i,--input <arg>    Path to the input jar.
 -o,--output <arg>   Destination to save the output graph (as graphml/dot/json).
 -q,--quiet          Don't print logs to console.
```
