.. meta::
    :author: Cask Data, Inc.
    :description: HTTP RESTful Interface to the Cask Data Application Platform
    :copyright: Copyright © 2015 Cask Data, Inc.

.. _http-restful-api-apptemplates:

==================================================
Application Template and Adapters HTTP RESTful API 
==================================================

Use the CDAP Application Template and Adapter HTTP API to obtain a list of available
application templates and plugins, and create, delete, and manage the lifecycle of
adapters.

Note that the ETL Templates are a type of application template, specifically designed for
creating ETL adapters. See the application templates :ref:`Introduction to Application Templates
and ETL <apptemplates-intro-application-templates>` for information on creating adapters and
operating them.

See the Developers’ Manual Advanced section on :ref:`Creating Application Templates
<advanced-custom-app-template>` for information on creating custom application templates,
plugins and adapters.


.. highlight:: console

Application Templates
=====================

.. _http-restful-api-apptemplates-available:

Available Application Templates 
-------------------------------
To retrieve a list of available application templates, submit an HTTP GET request::

  GET <base-url>/templates/

This will return a JSON String map that lists each application template with its name,
description and type of program that it creates. Example output (pretty-printed)::

  [
    {
      "name": "ETLBatch",
      "description": "Batch Extract-Transform-Load (ETL) Adapter",
      "programType": "Workflow"
    },
    {
      "name": "ETLRealtime",
      "description": "Real-Time Extract-Transform-Load (ETL) Adapter",
      "programType": "Worker"
    }
  ]


Template Details
----------------
To retrieve the details of a particular application templates, submit an HTTP GET request::

  GET <base-url>/templates/<template-id>
  
where

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Parameter
     - Description
   * - ``<template-id>``
     - Name of the application template, such as ``ETLBatch`` or ``ETLRealtime``
  
This will return a JSON String map that lists the details of the application template.
Example output of the ``ETLBatch`` application template (pretty-printed)::

  $ GET <base-url>/templates/ETLBatch

  {
    "extensions": [
        "sink",
        "source",
        "transform"
    ],
    "name": "ETLBatch",
    "description": "Batch Extract-Transform-Load (ETL) Adapter",
    "programType": "Workflow"
  }

``extensions`` is an array of the types of plugins that are available for use by the
application template.


.. _http-restful-api-apptemplates-update:

Template Update
---------------
To update a particular application template, submit an HTTP PUT request::

  PUT <base-url>/namespaces/<namespace-id>/templates/<template-id>
  
where

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Parameter
     - Description
   * - ``<namespace-id>``
     - Namespace ID of adapters that have used this application template
   * - ``<template-id>``
     - Name of the application template, such as ``ETLBatch`` or ``ETLRealtime``
  
This will cause an application template to be updated, and can be used if you are
deploying a custom JAR or plugin and need to update an application template so that your
changes are seen. Any other updates required (such as the re-creation of adapters based on that
Template) are left to the developer or user of the custom JAR or plugin.

Note that even though application templates are not namespaced, this particular call is
because it is governed by the adapters that use a Template. If different adapters in
different namespaces have used the same Template, multiple calls are needed to make sure
that any changes are promulgated. This is a behavior that will likely be addressed in a 
future release to improve this process.

Template Extensions 
-------------------
To retrieve a list of all the extensions of a particular type for an application
template, submit an HTTP GET request::

  GET <base-url>/templates/<template-id>/extensions/<extension-type>
  
where

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Parameter
     - Description
   * - ``<template-id>``
     - Name of the application template, such as ``ETLBatch`` or ``ETLRealtime``
   * - ``<extension-type>``
     - Extension type, such as (for ETL Templates) ``source``, ``sink``, or ``transform``

This will return a JSON String map that lists all the extensions of particular type for
that application template, including their name, description, and the source files that
contain the extension.

Example output for the ``source`` extensions of the ``ETLBatch`` application template
(pretty-printed and reformatted to fit):

.. container:: highlight

  .. parsed-literal::
    |$| GET <base-url>/templates/ETLBatch/extensions/source

    [
      {
        "template": {
          "name": "ETLBatch",
          "description": "Batch Extract-Transform-Load (ETL) Adapter",
          "programType": "Workflow"
        },
        "source": {
          "fileName": "cdap-etl-lib-|release|-batch.jar",
          "name": "cdap-etl-lib",
          "version": {
            "version": "|release|-batch",
            "major": |version-major|,
            "minor": |version-minor|,
            "fix": |version-fix|,
            "suffix": "|version-suffix-batch|"
          }
        },
        "type": "source",
        "name": "Database",
        "description": "Batch source for a database."
      },
      {
        "template": {
          "name": "ETLBatch",
          "description": "Batch Extract-Transform-Load (ETL) Adapter",
          "programType": "Workflow"
        },
        "source": {
          "fileName": "cdap-etl-lib-|release|-batch.jar",
          "name": "cdap-etl-lib",
          "version": {
            "version": "|release|-batch",
            "major": |version-major|,
            "minor": |version-minor|,
            "fix": |version-fix|,
            "suffix": "|version-suffix-batch|"
          }
        },
        "type": "source",
        "name": "KVTable",
        "description": "CDAP KeyValue Table Dataset Batch Source. Outputs records with a 
          'key' field and a 'value' field. Both fields are of type bytes."
      },
      {
        "template": {
          "name": "ETLBatch",
          "description": "Batch Extract-Transform-Load (ETL) Adapter",
          "programType": "Workflow"
        },
        "source": {
          "fileName": "cdap-etl-lib-|release|-batch.jar",
          "name": "cdap-etl-lib",
          "version": {
            "version": "|release|-batch",
            "major": |version-major|,
            "minor": |version-minor|,
            "fix": |version-fix|,
            "suffix": "|version-suffix-batch|"
          }
        },
        "type": "source",
        "name": "Stream",
        "description": "Batch source for a stream. If a format is given, any property 
          prefixed with 'format.setting.' will be passed to the format. For example, if a 
          property with key 'format.setting.delimiter' and value '|' is given, the setting 
          'delimiter' with value '|' will be passed to the format."
      },
      {
        "template": {
          "name": "ETLBatch",
          "description": "Batch Extract-Transform-Load (ETL) Adapter",
          "programType": "Workflow"
        },
        "source": {
          "fileName": "cdap-etl-lib-|release|-batch.jar",
          "name": "cdap-etl-lib",
          "version": {
            "version": "|release|-batch",
            "major": |version-major|,
            "minor": |version-minor|,
            "fix": |version-fix|,
            "suffix": "|version-suffix-batch|"
          }
        },
        "type": "source",
        "name": "Table",
        "description": "CDAP Table Dataset Batch Source"
      }
    ]


Details of an Extension (Plugin)
--------------------------------
To retrieve the details of an extension (plugin) used in an application template, submit
an HTTP GET request::

  GET <base-url>/templates/<template-id>/extensions/<extension-type>/plugins/<plugin-id>
  
where

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Parameter
     - Description
   * - ``<template-id>``
     - Name of the application template, such as ``ETLBatch`` or ``ETLRealtime``
   * - ``<extension-type>``
     - Extension type, such as (for ETL Templates) ``source``, ``sink``, or ``transform``
   * - ``<plugin-id>``
     - Plugin name

This will return a JSON String map that lists the details of the plugin. This is the
information needed when configuring an adapter using the plugin, the type of each
property, and whether it is a mandatory property (*"required"*).

Example output for the ``Database`` plugin of type ``source`` of the ``ETLBatch``
application template (pretty-printed and reformatted to fit):

.. container:: highlight

  .. parsed-literal::
    |$| GET <base-url>/templates/ETLBatch/extensions/source/plugins/Database

    [
      {
        "className": "co.cask.cdap.templates.etl.batch.sources.DBSource",
        "properties": {
          "jdbcPluginType": {
            "name": "jdbcPluginType",
            "description": "Type of the JDBC plugin to use. This is the value of the 'type' 
            key defined in the json file for the JDBC plugin. Defaults to 'jdbc'.",
            "type": "string",
            "required": false
          },
          "tableName": {
            "name": "tableName",
            "description": "Table name to export to.",
            "type": "string",
            "required": true
          },
          "jdbcPluginName": {
            "name": "jdbcPluginName",
            "description": "Name of the JDBC plugin to use. This is the value of the 'name' 
            key defined in the json file for the JDBC plugin. Defaults to 'jdbc'.",
            "type": "string",
            "required": false
          },
          "driverClass": {
            "name": "driverClass",
            "description": "Driver class to connect to the database.",
            "type": "string",
            "required": true
          },
          "importQuery": {
            "name": "importQuery",
            "description": "The SELECT query to use to import data from the specified table. 
            You can specify an arbitrary number of columns to import, or import all columns 
            using \*. You can also specify a number of WHERE clauses or ORDER BY clauses. 
            However, LIMIT and OFFSET clauses should not be used in this query.",
            "type": "string",
            "required": true
          },
          "connectionString": {
            "name": "connectionString",
            "description": "JDBC connection string including database name.",
            "type": "string",
            "required": true
          },
          "password": {
            "name": "password",
            "description": "Password to use to connect to the specified database. Required 
            for databases that need authentication. Optional for databases that do not 
            require authentication.",
            "type": "string",
            "required": false
          },
          "user": {
            "name": "user",
            "description": "User to use to connect to the specified database. Required for 
            databases that need authentication. Optional for databases that do not require 
            authentication.",
            "type": "string",
            "required": false
          },
          "countQuery": {
            "name": "countQuery",
            "description": "The SELECT query to use to get the count of records to import 
            from the specified table. Examples: SELECT COUNT(*) from <my_table> where 
            <my_column> 1, SELECT COUNT(my_column) from my_table). NOTE: Please include the 
            same WHERE clauses in this query as the ones used in the import query to reflect 
            an accurate number of records to import.",
            "type": "string",
            "required": true
          }
        },
        "template": {
          "name": "ETLBatch",
          "description": "Batch Extract-Transform-Load (ETL) Adapter",
          "programType": "Workflow"
        },
        "source": {
          "fileName": "cdap-etl-lib-|release|-batch.jar",
          "name": "cdap-etl-lib",
          "version": {
            "version": "|release|-batch",
            "major": |version-major|,
            "minor": |version-minor|,
            "fix": |version-fix|,
            "suffix": "|version-suffix-batch|"
          }
        },
        "type": "source",
        "name": "Database",
        "description": "Batch source for a database."
      }
    ]

.. _http-restful-api-apptemplates-adapters:

Adapters
========

.. _http-restful-api-apptemplates-adapters-creating:

Creating an Adapter 
-------------------
To create an adapter, submit an HTTP PUT request::

  PUT <base-url>/namespaces/<namespace-id>/adapters/<adapter-id>

with the path to the :ref:`adapter configuration file
<apptemplates-etl-configuration-file-format>` as the body of the request::

  <config-path>

where

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Parameter
     - Description
   * - ``<namespace-id>``
     - Namespace ID
   * - ``<adapter-id>``
     - Name of the adapter
   * - ``<config-path>``
     - Path to the configuration file

The format of the configuration file is described in the application templates section
on :ref:`Creating an ETL Adapter <apptemplates-etl-configuration-file-format>`.

.. rubric::  Example

.. list-table::
   :widths: 20 80
   :stub-columns: 1

   * - HTTP Method
     - ``PUT <base-url>/namespaces/default/adapters/streamAdapter -d @config.json``
   * - Description
     - Creates an adapter *streamAdapter* in the namespace *default* using the configuration
       file ``config.json``

.. _http-restful-api-apptemplates-adapters-listing:

Listing Existing Adapters
-------------------------
To retrieve a list of the existing adapters, submit an HTTP GET request::

  GET <base-url>/namespaces/<namespace-id>/adapters

where

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Parameter
     - Description
   * - ``<namespace-id>``
     - Namespace ID

This will return a JSON String map that lists all the current adapters and all of their details.

.. highlight:: console

For example, if an adapter *streamAdapter* has been created as in the previous command, the
command will return a list of adapters (pretty-printed and reformatted to fit)::

  [
    {
      "name": "streamAdapter",
      "description": "Batch ETL",
      "template": "ETLBatch",
      "program": {
        "namespace": "default",
        "application": "ETLBatch",
        "type": "Workflow",
        "id": "ETLWorkflow"
      },
      "config": {
        "schedule": "* * * * *",
        "source": {
          "name": "Stream",
          "properties": {
              "name": "myStream",
              "duration": "1m"
          }
        },
        "sink": {
          "name": "Table",
          "properties": {
              "name": "myTable",
              "schema.row.field": "ts"
          }
        },
        "transforms": [

        ]
      },
      "schedule": {
        "schedule": {
          "cronExpression": "* * * * *",
          "name": "streamAdapter.etl.batch.adapter.streamAdapter.schedule",
          "description": "Schedule for streamAdapter Adapter"
        },
        "program": {
          "programName": "ETLWorkflow",
          "programType": "WORKFLOW"
        },
        "properties": {
          "transformIds": "[]",
          "name": "streamAdapter",
          "sinkId": "sink:Table",
          "config": "{\"schedule\":\"* * * * *\",\"source\":{\"name\":\"Stream\",
          \"properties\":{\"duration\":\"1m\",\"name\":\"myStream\"}},\"sink\":{\"name\":
          \"Table\",\"properties\":{\"name\":\"myTable\",\"schema.row.field\":\"ts\"}},
          \"transforms\":[]}",
          "sourceId": "source:Stream"
        }
      },
      "instances": 1
    }
  ]

.. _http-restful-api-apptemplates-adapters-details:

List Details of an Adapter
--------------------------
To retrieve the details of a particular adapter, submit an HTTP GET request::

  GET <base-url>/namespaces/<namespace-id>/adapters/<adapter-id>

where

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Parameter
     - Description
   * - ``<namespace-id>``
     - Namespace ID
   * - ``<adapter-id>``
     - Name of the adapter


For example, if an adapter *streamAdapter* has been created as in a previous command, the
command will return (pretty-printed and reformatted to fit)::

  {
    "name": "streamAdapter",
    "description": "Batch ETL",
    "template": "ETLBatch",
    "program": {
      "namespace": "default",
      "application": "ETLBatch",
      "type": "Workflow",
      "id": "ETLWorkflow"
    },
    "config": {
      "schedule": "* * * * *",
      "source": {
        "name": "Stream",
        "properties": {
            "name": "myStream",
            "duration": "1m"
        }
      },
      "sink": {
        "name": "Table",
        "properties": {
            "name": "myTable",
            "schema.row.field": "ts"
        }
      },
      "transforms": [

      ]
    },
    "schedule": {
      "schedule": {
        "cronExpression": "* * * * *",
        "name": "streamAdapter.etl.batch.adapter.streamAdapter.schedule",
        "description": "Schedule for streamAdapter Adapter"
      },
      "program": {
        "programName": "ETLWorkflow",
        "programType": "WORKFLOW"
      },
      "properties": {
        "transformIds": "[]",
        "name": "streamAdapter",
        "sinkId": "sink:Table",
        "config": "{\"schedule\":\"* * * * *\",\"source\":{\"name\":\"Stream\",
        \"properties\":{\"duration\":\"1m\",\"name\":\"myStream\"}},\"sink\":{\"name\":
        \"Table\",\"properties\":{\"name\":\"myTable\",\"schema.row.field\":\"ts\"}},
        \"transforms\":[]}",
        "sourceId": "source:Stream"
      }
    },
    "instances": 1
  }


Status of an Adapter
--------------------
To retrieve the status of an adapter, submit an HTTP GET request::

  GET <base-url>/namespaces/<namespace-id>/adapters/<adapter-id>/status

where

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Parameter
     - Description
   * - ``<namespace-id>``
     - Namespace ID
   * - ``<adapter-id>``
     - Name of the adapter

It will return the status of the adapter, one of ``STOPPED``, ``STARTING``, ``STARTED``.

If there is an error (for instance, the adapter does not exist), a message and an
appropriate status code (``404``) will be returned.

Starting an Adapter
-------------------
Starting a batch adapter schedules a workflow to be run periodically based on the cron
schedule that is configured in the adapter. Starting a real-time adapter starts a CDAP
worker.

To start an adapter, submit an HTTP POST request::

  POST <base-url>/namespaces/<namespace-id>/adapters/<adapter-id>/start

where

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Parameter
     - Description
   * - ``<namespace-id>``
     - Namespace ID
   * - ``<adapter-id>``
     - Name of the adapter

Stopping an Adapter
-------------------
To stop an adapter, submit an HTTP POST request::

  POST <base-url>/namespaces/<namespace-id>/adapters/<adapter-id>/stop

where

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Parameter
     - Description
   * - ``<namespace-id>``
     - Namespace ID
   * - ``<adapter-id>``
     - Name of the adapter

Deleting an Adapter
-------------------
To delete an adapter, submit an HTTP DELETE request::

  DELETE <base-url>/namespaces/<namespace-id>/adapters/<adapter-id>

where

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Parameter
     - Description
   * - ``<namespace-id>``
     - Namespace ID
   * - ``<adapter-id>``
     - Name of the adapter

Retrieving Adapter Runs
-----------------------
To retrieve a list of runs of an adapter, submit an HTTP GET request::

  GET <base-url>/namespaces/<namespace-id>/adapters/<adapter-id>/runs

where

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Parameter
     - Description
   * - ``<namespace-id>``
     - Namespace ID
   * - ``<adapter-id>``
     - Name of the adapter

The command will return a list of runs for the adapter (pretty-printed and reformatted to
fit)::

  [
    {
      "runid": "f0697b83-ef7e-11e4-8f65-22d805694e6a",
      "start": 1430428920,
      "end": 1430428922,
      "status": "FAILED",
      "adapter": "streamAdapter",
      "properties": {
        "0": "f06eaba4-ef7e-11e4-9586-22d805694e6a"
      }
    },
    {
      "runid": "cc502641-ef7e-11e4-b47a-22d805694e6a",
      "start": 1430428860,
      "end": 1430428869,
      "status": "COMPLETED",
      "adapter": "streamAdapter",
      "properties": {
        "0": "cc5b49d2-ef7e-11e4-9c24-22d805694e6a"
      }
    }
  ]


Retrieving Adapter Logs
-----------------------
As an adapter is an instantiation of a particular program (a workflow, MapReduce, workers, etc.),
the logs for an adapter are the logs of the underlying program. To retrieve these logs
using a RESTful API, you need to know which underlying program the adapter uses
and then use the CDAP :ref:`Logging API <http-restful-api-logging>` to retrieve its logs.

To find the underlying programs, you can :ref:`list details of an adapter
<http-restful-api-apptemplates-adapters-details>` and then use its ``program`` information
to determine how to build your request::

    "program": {
      "namespace": "default",
      "application": "ETLBatch",
      "type": "Workflow",
      "id": "ETLWorkflow"
    },

For example, using the previous ``streamAdapter``, you would be interested in the logs of the
workflow *ETLWorkflow* of the application *ETLBatch* of the namespace *default*. From this,
you can formulate your request.

The :ref:`CDAP CLI <cli>` has a command (``get adapter logs <adapter-id>``) that does this directly.

.. _http-restful-api-apptemplates-adapter-metrics:

Retrieving Adapter Metrics
--------------------------
To retrieve the metrics of an adapter, use these RESTful API endpoints.


.. rubric:: Find Available Adapters

To search for the available adapters, if metrics have been emitted by the adapters, submit an HTTP GET request::

  GET <base-url>/metrics/search?target=tags&tag=namespace:<namespace-id>

where

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Parameter
     - Description
   * - ``<namespace-id>``
     - Namespace ID

The command will return available adapters in *namespace-id* if metrics have been emitted by the adapters.


.. rubric:: Find Available Metrics

To search for the available metrics for an adapter, submit an HTTP GET request::

  GET <base-url>/metrics/search?target=metric&tag=namespace:<namespace-id>&tag=adapter:<adapter-id>

where

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Parameter
     - Description
   * - ``<namespace-id>``
     - Namespace ID
   * - ``<adapter-id>``
     - Adapter ID


.. rubric:: Aggregate Available Values

To retrieve the aggregated value for a metric emitted by an adapter, submit an HTTP GET request::

  GET <base-url>/metrics/query?tag=namespace:<namespace-id>&tag=adapter:<adapter-id>&metric=<metric-id>&aggregate=true

where

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Parameter
     - Description
   * - ``<namespace-id>``
     - Namespace ID
   * - ``<adapter-id>``
     - Adapter ID
   * - ``<metric-id>``
     - Metric ID

The command will return the aggregate value for the metric *metric-id* emitted by *adapter-id* in
*namespace-id* across all runs of the adapter. If you would like the metrics for a
particular run, specify an additional tag of ``tag=run:<run-id>`` in the above query.
