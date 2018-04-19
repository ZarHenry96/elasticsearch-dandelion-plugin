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
```
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

To take advantage of Dandelion plugin's functionalities you need an authorization token, which you can get by subscribing to [dandelion](https://dandelion.eu/accounts/register/?next=/docs/api/datatxt/nex/v1/).\
Once you have it, you can start to use Dandelion anlayzer ('dandelion-a').

## Usage example

#### Index creation

First of all you have to create an index.\
**NOTE**: to make this code work, you have to replace the value of the field "auth" in "dandelion_analyzer" with your authorization token.
```bash
curl -XPUT 'localhost:9200/dandelion?pretty' -H 'Content-Type: application/json' -d'
{
   "settings":{
      "analysis":{
         "analyzer":{
            "dandelion_analyzer":{
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
               "analyzer":"dandelion_analyzer",
               "search_analyzer":"dandelion_analyzer"
            }
         }
      }
   }
}
'
```
By doing so you create an index (named dandelion) that uses Dandelion analyzer ('dandelion-a') on the field "text" of documents of type "_doc" for both indexing and searching.\
Moreover, it is not necessary to define the parameter "lang", which is set to "auto" by default.

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

Finally, you can search this document taking advantage of Dandelion's entity extraction service. \
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

**NOTE:** you have to specify "the painter" in the query text to allow the analyzer to recognize the language used and the concept alluded to.
Indeed the language is essential for entities identification. Also, the same entity mentioned in different languages won't be matched if the option "multilang" is not enabled (see Parameters section below).

## Parameters

Dandelion analyzer accepts three parameters:

1. **auth** (required) : Dandelion authorization token. It is needed to perform requests to Dandelion API, that is for entity extraction. (tokenizer parameter)
2. **lang** (optional) : input text language. It set to "auto" by default. See [languages](https://dandelion.eu/docs/api/datatxt/nex/v1/#param-lang) for allowed languages. (tokenizer parameter)
3. **multilang** (optional): multilanguage function. The allowed values are "true" and "false" (the default is "false"). See below for usage details. (token filter parameter)

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
               "auth":"authorization token",
               "multilang":"true"
            },
            "dandelion_search_analyzer":{
               "type": "dandelion-a",
               "auth":"authorization token"
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
               "search_analyzer":"dandelion_search_analyzer"
            }
         }
      }
   }
}
'
```

<br>After that insert a new document mentioning the Mona Lisa in italian:

```bash
curl -XPUT 'http://localhost:9200/dandelion_multilanguage/_doc/gioconda' -H 'Content-Type: application/json' -d'
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
    * **Compile** <br> To compile the plugin, you have to run the command `./gradlew assemble` from the plugin root directory.\
    You will find the zip under the path `build/distributions`.\
    NOTE: to compile it you need Gradle 4.6 and JVM 9; you can set the elasticsearch target version in gradle.properties.

    * **Download** <br> To download it, you can choose the desired version from here: [releases](https://github.com/ZarHenry96/elasticsearch-dandelion-plugin/releases).
2. Once you have the zip, you have to install it:
    * To do it, run the command: `./bin/elasticsearch-plugin install file:///path/to/zip`.

    * Alternatively, you can unzip it and place what you have extracted into elasticsearch's plugins folder.

## Uninstall

If you want to uninstall Dandelion plugin, run this command from the elasticsearch root directory:
```bash
./bin/elasticsearch-plugin remove dandelion
```
