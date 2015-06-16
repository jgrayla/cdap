.. meta::
    :author: Cask Data, Inc.
    :copyright: Copyright © 2015 Cask Data, Inc.

.. _stream-exploration:

==================
Stream Exploration
==================

Streams are the primary method of ingesting real-time data into CDAP.
It is often useful to be able to examine data in a stream in an ad-hoc manner through
SQL-like queries 

Each event in a stream contains a timestamp, a map of headers, and a body. When a stream
is created, a corresponding Hive table is created that allows queries to be run over
those three columns. Many times, stream event bodies are also structured and have
a format and schema of their own. For example, event bodies may be comma-delimited
text or Avro-encoded binary data. In those cases, it is possible to set a format and schema
on a stream, enabling more powerful queries.

Let's take a closer look at attaching format and schema on streams.

Stream Format
-------------

A format defines how the bytes in an event body can be read as a higher level object.
For example, the CSV (comma separated values) format can read each value in comma-delimited text 
as a separate column of some given type. Each format has 
a schema that describes the structure of data the format can read. Some formats, such as the Avro format,
require a schema to be explicitly given. Other formats, such as the CSV format, have a default schema. 

Format is a configuration setting that can be set on a stream. This can be done either through the
HTTP RESTful API or by using the CDAP Command Line Interface (CLI)::

  set stream format <stream-id> <format-name> [<schema>] [<settings>]
  set stream format mystream csv "f1 int, f2 string"

As mentioned above, a format may support different schemas, which will be discussed in more detail
in the next section.

It is important to note that formats are applied at read time.
When events are added to a stream, there are no checks performed to enforce that
all events added to the stream are readable by the format you have set on a stream.
If any stream event cannot be read by the format you have set, your entire query will fail and you
will not get any results.

Accepted formats are ``avro``, ``csv`` (comma-separated), ``tsv`` (tab-separated), ``text``, ``clf``, 
``grok``, and ``syslog``.

Schema
------
CDAP schemas are adopted from the `Avro Schema Declaration <http://avro.apache.org/docs/1.7.3/spec.html#schemas>`__
with a few differences:
 
  * Map keys do not have to be strings, but can be of any type.
  * No "name" property for the enum type. 
  * No support of "doc" and "aliases" in record and enum types.
  * No support of "doc" and "default" in record fields.
  * No "fixed" type.

There are a few additional limitations on the types of schemas that can be used for exploration: 
 
  * Schemas must be a record of at least one field.
  * Enums are not supported.
  * Unions are not supported, unless it is a union of a null and another type, representing a nullable type.
  * Recursive types are not supported. This means you cannot have a record field that references itself in one of its fields.

Data exploration using Impala has additional limitations:

  * Fields must be a scalar type (no maps, arrays, or records).

On top of these general limitations, each format has its own restrictions on the types
of schemas they support. For example, the CSV format does not support maps or records as
data types.

Schema Syntax
-------------
Schemas are represented as JSON Objects, following the same format as `Avro schemas
<http://avro.apache.org/docs/1.7.3/spec.html#schemas>`__. 
The JSON representation is used by the HTTP RESTful APIs, while the CDAP CLI supports a SQL-like syntax.

For example, the SQL-like schema::

  f1 int, f2 string, f3 array<int> not null, f4 map<string, int> not null, f5 record<x:int, y:double> not null

is equivalent to the Avro-like JSON schema::

  {
    "type": "record",
    "name": "rec",
    "fields": [
      { 
        "name": "f1",
        "type": [ "int", "null" ]
      },
      { 
        "name": "f2",
        "type": [ "string", "null" ]
      },
      { 
        "name": "f3",
        "type": { "type": "array", "items": [ "int", "null" ] } 
      },
      { 
        "name": "f4", 
        "type": { 
          "type": "map", "keys": [ "string", "null" ], "values": [ "int", "null" ] 
        }
      },
      { 
        "name": "f5",
        "type": {
          "type": "record",
          "name": "rec1",
          "fields": [
            { "name": "x", "type": [ "int", "null" ] },
            { "name": "y", "type": [ "double", "null" ] }
          ]
        }
      }
    ]
  }

Text Format
-----------
The ``text`` format simply interprets each event body as a string. The format supports a very limited
schema, namely a record with just one field of type ``string``. The format supports a ``charset`` setting
that allows you to specify the charset of the text. It defaults to ``utf-8``.

For example::

  set stream format mystream text "data string not null" "charset=ISO-8859-1"

CSV and TSV Formats
-------------------
The ``csv`` (comma separated values) and ``tsv`` (tab separated values) formats read event bodies as delimited text.
They have two settings, ``charset`` for the text charset, and ``delimiter`` for the delimiter.
The ``charset`` setting defaults to ``utf-8``. The ``delimiter`` setting defaults to a comma
for the ``csv`` format and to a tab for the ``tsv`` format.

These formats only support scalars as column types, except for the very last column, which can be an array of strings.
All types can be nullable. If no schema is given, the default schema is an array of strings. 

For example::
 
  set stream format mystream csv "col1 string, col2 int not null, col3 array<string>"

Avro Format
-----------
The ``avro`` format reads event bodies as binary encoded Avro. The format requires a schema to be given,
and has no settings.

For example::

  set stream format mystream avro "col1 string, col2 map<string,int> not null, col3 record<x:double, y:float>"

End-to-end Example
------------------

In the following example, we will create a stream, send data to it, attach a format
and schema to the stream, then query the stream.

Suppose we want to create a stream for stock trades. We first create the stream
and send some data to it as comma-delimited text::

  > create stream trades
  > send stream trades "AAPL,50,112.98"
  > send stream trades "AAPL,100,112.87"
  > send stream trades "AAPL,8,113.02"
  > send stream trades "NFLX,10,437.45"

If we run a query over the stream, we can see each event as text::

  > execute "select * from stream_trades"
  +===================================================================================================+
  | stream_trades.ts: BIGINT | stream_trades.headers: map<string,string> | stream_trades.body: STRING |
  +===================================================================================================+
  | 1422493022983            | {}                                        | AAPL,50,112.98             |
  | 1422493027358            | {}                                        | AAPL,100,112.87            |
  | 1422493031802            | {}                                        | AAPL,8,113.02              |
  | 1422493036080            | {}                                        | NFLX,10,437.45             |
  +===================================================================================================+

Since we know the body of every event is comma separated text and that each event
contains three fields, we can set a format and schema on the stream to allow us to run more
complicated queries::

  > set stream format trades csv "ticker string, num_traded int, price double"
  > execute "select ticker, count(*) as transactions, sum(num_traded) as volume from stream_trades group by ticker order by volume desc" 
  +========================================================+
  | ticker: STRING | transactions: BIGINT | volume: BIGINT |
  +========================================================+
  | AAPL           | 3                    | 158            |
  | NFLX           | 1                    | 10             |
  +========================================================+

Formulating Queries
-------------------
When creating your queries, keep these limitations in mind:

- The query syntax of CDAP is a subset of the variant of SQL that was first defined by Apache Hive.
- Writing into a stream using SQL is not supported.
- The SQL command ``DELETE`` is not supported.
- When addressing your streams in queries, you need to prefix the stream name with
  ``stream_``. For example, if your stream is named ``Purchases``, then the corresponding table
  name is ``stream_purchases``. Note that the table name is all lower-case, regardless of how it was defined.
- If your stream name contains a '.' or a '-', those characters will be converted to '_' for the Hive table name.
  For example, if your stream is named ``my-stream.name``, the corresponding Hive table name will be ``stream_my_stream_name``.
  Beware of name collisions. For example, ``my.stream`` will use the same Hive table name as ``my_stream``.
- CDAP uses a custom storage handler to read streams through Hive. This means that queries must be run through
  CDAP and not directly through Hive unless you place CDAP jars in your Hive classpath. This also means that
  streams cannot be queried directly by Impala. If you wish to use Impala to explore data in a stream, you can
  create an :ref:`adapter <apptemplates-index>` that converts stream data into a ``TimePartitionedFileSet``. 
- Some versions of Hive may try to create a temporary staging directory at the table location when executing queries.
  If you are seeing permission errors, try setting ``hive.exec.stagingdir`` in your Hive configuration to ``/tmp/hive-staging``.

For more examples of queries, please refer to the `Hive language manual
<https://cwiki.apache.org/confluence/display/Hive/LanguageManual>`__.
