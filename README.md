# Dandelion analysis plugin for Elasticsearch

[![Download](resources/download.svg)](https://github.com/ZarHenry96/elasticsearch-dandelion-plugin/releases/download/v6.2.4/dandelion-6.2.4.zip)

Dandelion plugin integrates the Named Entity Extraction service offered by SpazioDati through Dandelion API into elasticsearch.\
It provides an analyzer ('dandelion-a') that allows semantic searches based on the entities extracted from the input texts.\
The analyzer is made up of a tokenizer ('dandelion-t') and a token filter ('dandelion-tf'), which can be used independently but have been designed to work together.

## Versions

| Dandelion Plugin Version | ES Version |
|-------|-------|
| `6.2.4` | `6.2.4` |
| `6.2.3` | `6.2.3` |

## Quick start 

To install Dandelion plugin run this command from the elasticsearch root directory:
```bash
./bin/elasticsearch-plugin install https://github.com/ZarHenry96/elasticsearch-dandelion-plugin/releases/download/v6.2.4/dandelion-6.2.4.zip
```
**NOTE**: replace 6.2.4 with the desired version

During installation you will be asked for additional permissions:

```
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@     WARNING: plugin requires additional permissions     @
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
* java.lang.RuntimePermission accessClassInPackage.sun.misc
* java.lang.RuntimePermission accessClassInPackage.sun.reflect
* java.lang.RuntimePermission accessDeclaredMembers
* java.lang.RuntimePermission reflectionFactoryAccess
* java.lang.RuntimePermission setFactory
* java.lang.reflect.ReflectPermission suppressAccessChecks
* java.net.SocketPermission api.dandelion.eu:443 connect,resolve
* java.net.SocketPermission it.wikipedia.org:443 connect,resolve
See http://docs.oracle.com/javase/8/docs/technotes/guides/security/permissions.html
for descriptions of what these permissions allow and the associated risks.

Continue with installation? [y/N]
```
These permissions are necessary for the correct functioning of the plugin and tests, so type y and press Enter.

**Finally, be sure to restart Elasticsearch**.

**NOTE**: if you are interested in installation details, see [More on installation procedure](#more-on-installation-procedure)

## Authorization

To take advantage of Dandelion plugin's functionalities you need an authorization token, which you can get by subscribing to [dandelion](https://dandelion.eu/accounts/register/?next=/docs/api/datatxt/nex/v1/).

Once you get it, you have to define the "dandelion.auth" setting in elasticsearch keystore. To do so:
 1. If you haven't created the keystore yet, first of all run this command from the elasticsearch root directory:
    ```bash
    ./bin/elasticsearch-keystore create
    ```
 2. Then add "dandelion.auth" setting to the keystore through the command:
    ```bash
    ./bin/elasticsearch-keystore add dandelion.auth
    ```
    You will be asked for "dandelion.auth" value: insert your authorization token and press Enter.

To change "dandelion.auth" value repeat point 2.
Now you are ready to use "dandelion-a" analyzer, but note that **all the modifications to the keystore take affect only after restarting Elasticsearch**.

**NOTE**: you can also not set "dandelion.auth" in the keystore, but then you will have to specify the "auth" parameter in all the requests (analyze,index creation...) to take advantage of dandelion's functionalities. For example (index creation):
```bash
curl -XPUT 'localhost:9200/index_name?pretty' -H 'Content-Type: application/json' -d'
{
   "settings":{
      "analysis":{
         "analyzer":{
            "analyzer_name":{
               "type":"dandelion-a",
               "auth":"authorization token",
               "lang":"auto"
            }
         }
      }
   },
   "mappings":{
      "_doc":{
         "properties":{
            "text": {
               "type":"text",
               "analyzer":"analyzer_name",
               "search_analyzer":"analyzer_name"
            }
         }
      }
   }
}
'
```
To make this code work, you have to replace the value of the field "auth" in "dandelion_analyzer" with your authorization token, but **it's not recommended for elasticsearch instances that are not local**.

**Eventually**, if you set both "dandelion.auth" in the keystore and "auth" in the index, the "auth" parameter defined in the index settings will have priority.

## Usage example

#### Index creation

First of all you have to create an index. For example: 
```bash
curl -XPUT 'localhost:9200/dandelion?pretty' -H 'Content-Type: application/json' -d'
{
   "mappings":{
      "_doc":{
         "properties":{
            "text": {
               "type":"text",
               "analyzer":"dandelion-a",
               "search_analyzer":"dandelion-a"
            }
         }
      }
   }
}
'
```
By doing so you create an index (named dandelion) that uses Dandelion analyzer ('dandelion-a') on the field "text" of documents of type "_doc" for both indexing and searching.

#### Document insertion

Now you can insert documents. For instance:
```bash
curl -XPUT 'http://localhost:9200/dandelion/_doc/gioconda?pretty' -H 'Content-Type: application/json' -d'
{
    "text":"The Mona Lisa, painted by Leonardo."
}
'
```

#### Search (query)

Finally, you can search this document taking advantage of Dandelion's entity extraction service.\
Running a query like this you will find the document inserted previously:
```bash
curl -XGET 'localhost:9200/dandelion/_search?pretty' -H 'Content-Type: application/json' -d'
{
    "query": {
        "match" : {
            "text" : "the painter Da Vinci"
        }
    }
}
'
```
In fact "da Vinci" is recognized as the italian painter Leonardo da Vinci, mentioned before as "Leonardo".\
Here is the result:
```
{
  "took" : 266,
  "timed_out" : false,
  "_shards" : {
    "total" : 5,
    "successful" : 5,
    "skipped" : 0,
    "failed" : 0
  },
  "hits" : {
    "total" : 1,
    "max_score" : 0.2876821,
    "hits" : [
      {
        "_index" : "dandelion",
        "_type" : "_doc",
        "_id" : "gioconda",
        "_score" : 0.2876821,
        "_source" : {
          "text" : "The Mona Lisa, painted by Leonardo."
        }
      }
    ]
  }
}

```

**NOTE:** you have to specify "the painter" in the query text to allow the analyzer to recognize the language used (and the concept alluded to).
Indeed the language is essential for entities identification. Also, the same entity mentioned in different languages won't be matched if the option "multilang" is not enabled (see Parameters section below).

## Parameters

Dandelion analyzer accepts three parameters:

1. **auth** : Dandelion authorization token. It is used to perform requests to Dandelion API, that is for entity extraction. (required only if "dandelion.auth" is not defined in the keystore -> see [Authorization](#authorization); tokenizer parameter)
2. **lang** : input text language. It set to "auto" (automatic recognition) by default. See [languages](https://dandelion.eu/docs/api/datatxt/nex/v1/#param-lang) for allowed languages. (optional; tokenizer parameter)
3. **multilang**: multilanguage function. The allowed values are "true" and "false" (the default is "false"). See below for usage details. (optional; token filter parameter)

## Multilanguage

The multilanguage functionality permits the recognition of an entity mentioned in different languages through the use of Wikipedia APIs.\
Enabling it, each entity identified in a specific language is converted in all the languages supported by dandelion and wikipedia.\
This is particularly useful if applied during indexing phase. 

##### Example

First of all define an index ('dandelion_multilanguage') with different analyzers, one for indexing (with multilanguage option enabled) and one for searching (without multilanguage):
```bash
curl -XPUT 'localhost:9200/dandelion_multilanguage?pretty' -H 'Content-Type: application/json' -d'          
{
   "settings":{                              
      "analysis":{
         "analyzer":{
            "dandelion_index_analyzer":{
               "type": "dandelion-a",
               "multilang":"true"
            }
         }
      }
   },
   "mappings":{
      "_doc":{
         "properties":{
            "text": {
               "type":"text",
               "analyzer":"dandelion_index_analyzer",
               "search_analyzer":"dandelion-a"
            }
         }
      }
   }
}
'
```

<br>After that insert a new document mentioning the Mona Lisa in italian:

```bash
curl -XPUT 'http://localhost:9200/dandelion_multilanguage/_doc/gioconda?pretty' -H 'Content-Type: application/json' -d'
{
   "text":"La Gioconda, dipinta da Leonardo."  
}                 
'       
```
By doing so, the entities extracted (the "Mona Lisa" and "Leonardo da Vinci") are indexed in all available and supported languages.

<br>Now, if you run a query searching for "The Mona Lisa", thanks to the multilanguage option you will find the document you have just inserted:
```bash
curl -XGET 'localhost:9200/dandelion_multilanguage/_search?pretty' -H 'Content-Type: application/json' -d'
{
    "query": {
        "match" : {                                               
            "text" : "The Mona Lisa"                   
        }
    }
}
'
```
Here is the result:
```
{
  "took" : 193,
  "timed_out" : false,
  "_shards" : {
    "total" : 5,
    "successful" : 5,
    "skipped" : 0,
    "failed" : 0
  },
  "hits" : {
    "total" : 1,
    "max_score" : 0.4801315,
    "hits" : [
      {
        "_index" : "dandelion_multilanguage",
        "_type" : "_doc",
        "_id" : "gioconda",
        "_score" : 0.4801315,
        "_source" : {
          "text" : "La Gioconda, dipinta da Leonardo."
        }
      }
    ]
  }
}
```

## More on installation procedure

There are several ways to install an elasticsearch plugin.\
The fastest is the one shown in quick start, which combines these two steps into one command:

1. To install Dandelion plugin you have first to compile or download it:
    * **Compile** <br> To compile the plugin, you have to run this command from the plugin root directory.
        ```bash
        ./gradlew assemble
        ```
        You will find the zip under the path `build/distributions`.\
        NOTE: you can set the elasticsearch target version and the jdk version in gradle.properties.

    * **Download** <br> To download it, you can choose the desired version from here: [releases](https://github.com/ZarHenry96/elasticsearch-dandelion-plugin/releases).
2. Once you have the zip, you have to install it:
    * To do it, run the command:
        ```bash
        ./bin/elasticsearch-plugin install file:///path/to/zip
        ```

    * Alternatively, you can unzip it and place what you have extracted into elasticsearch's plugins folder.

## Uninstall

If you want to uninstall Dandelion plugin, first of all run this command from the elasticsearch root directory:
```bash
./bin/elasticsearch-plugin remove dandelion
```

Then, to remove "dandelion.auth" from the keystore run:
```bash
./bin/elasticsearch-keystore remove dandelion.auth
```
