.. meta::
    :author: Cask Data, Inc.
    :copyright: Copyright © 2014-2015 Cask Data, Inc.

.. _client-api:

.. _java-client-api:

===============
Java Client API
===============

The Cask Data Application Platform (CDAP) Java Client API provides methods for interacting
with CDAP from Java applications.

Maven Dependency
----------------

.. highlight:: console

To use the Java Client API in your project, add this Maven dependency::

  <dependency>
    <groupId>co.cask.cdap</groupId>
    <artifactId>cdap-client</artifactId>
    <version>${cdap.version}</version>
  </dependency>

.. highlight:: java

Components
----------

The Java Client API allows you to interact with these CDAP components:

- `ApplicationClient: <#application-client>`_ interacting with applications
- `ProgramClient: <#program-client>`_ interacting with flows, MapReduce programs, user services, workflows, and workers
- `StreamClient: <#stream-client>`_ interacting with streams
- `DatasetClient: <#dataset-client>`_ interacting with datasets
- `DatasetModuleClient: <#dataset-module-client>`_ interacting with dataset Modules
- `DatasetTypeClient: <#dataset-type-client>`_ interacting with dataset Types
- `QueryClient: <#query-client>`_ querying datasets
- `ServiceClient: <#service-client>`_ interacting with user services
- `MetricsClient: <#metrics-client>`_ interacting with metrics
- `MonitorClient: <#monitor-client>`_ monitoring system services
- `PreferencesClient: <#preferences-client>`_ interacting with preferences

Alphabetical list:

- `ApplicationClient: <#application-client>`_ interacting with applications
- `DatasetClient: <#dataset-client>`_ interacting with datasets
- `DatasetModuleClient: <#dataset-module-client>`_ interacting with dataset Modules
- `DatasetTypeClient: <#dataset-type-client>`_ interacting with dataset Types
- `MetricsClient: <#metrics-client>`_ interacting with metrics
- `MonitorClient: <#monitor-client>`_ monitoring system services
- `PreferencesClient: <#preferences-client>`_ interacting with preferences
- `ProgramClient: <#program-client>`_ interacting with flows, MapReduce Programs, user services, workflows, and workers
- `QueryClient: <#query-client>`_ querying datasets
- `ServiceClient: <#service-client>`_ interacting with user services
- `StreamClient: <#stream-client>`_ interacting with streams

The above lists link to the examples below for each portion of the API.

.. _client-api-configuring-client:

Configuring your \*Client
-------------------------

Every *\*Client* constructor requires a `ClientConfig` instance which configures the hostname and port of the CDAP
instance that you wish to interact with.

In a non-secure (default) CDAP instance, instantiate as follows::

  // Interact with the CDAP instance located at example.com, port 10000
  ClientConfig clientConfig = new ClientConfig("example.com", 10000);

In a secure CDAP instance, first pull in the ``cdap-authentication-client`` Maven dependency::

  <dependency>
    <groupId>co.cask.cdap</groupId>
    <artifactId>cdap-authentication-client</artifactId>
    <version>${cdap.client.version}</version>
  </dependency>

Then, instantiate as follows::

  // Obtain AccessToken
  AuthenticationClient authenticationClient = new BasicAuthenticationClient();
  authenticationClient.setConnectionInfo("example.com", 10000, sslEnabled);
  // Configure the AuthenticationClient as documented in
  // https://github.com/caskdata/cdap-clients/blob/develop/cdap-authentication-clients/java
  AccessToken accessToken = authenticationClient.getAccessToken();

  // Interact with the secure CDAP instance located at example.com, port 10000, with the provided accessToken
  ClientConfig clientConfig = new ClientConfig("example.com", 10000, accessToken);

.. _application-client:

ApplicationClient
-----------------
::

  ClientConfig clientConfig;

  // Construct the client used to interact with CDAP
  ApplicationClient appClient = new ApplicationClient(clientConfig);

  // Fetch the list of applications
  List<ApplicationRecord> apps = appClient.list();

  // Deploy an application
  File appJarFile = ...;
  appClient.deploy(appJarFile);

  // Delete an application
  appClient.delete("Purchase");

  // List programs belonging to an application
  appClient.listPrograms("Purchase");

.. _preferences-client:

PreferencesClient
-----------------
::

  ClientConfig clientConfig;

  // Construct the client used to interact with CDAP
  PreferencesClient preferencesClient = new PreferencesClient(clientConfig);

  Map<String, String> propMap = Maps.newHashMap();
  propMap.put("k1", "v1");

  // Set preferences at the Instance level
  preferencesClient.setInstancePreferences(propMap);

  // Get preferences at the Instance level
  Map<String, String> currentPropMap = preferencesClient.getInstancePreferences();

  // Delete preferences at the Instance level
  preferencesClient.deleteInstancePreferences();

  // Set preferences of MyApp application which is deployed in the Dev namespace
  preferencesClient.setApplicationPreferences("Dev", "MyApp", propMap);

  // Get only the preferences of MyApp application which is deployed in the Dev namespace
  Map<String, String> appPrefs = preferencesClient.getApplicationPreferences("Dev", "MyApp", false);

  // Get the resolved preferences (collapsed with higher level(s) of preferences)
  Map<String, String> resolvedAppPrefs = preferencesClient.getApplicationPreferences("Dev", "MyApp", true);

.. _program-client:

ProgramClient
-------------
::

  ClientConfig clientConfig;

  // Construct the client used to interact with CDAP
  ProgramClient programClient = new ProgramClient(clientConfig);

  // Start a service in the WordCount example
  programClient.start("WordCount", ProgramType.SERVICE, "RetrieveCounts");

  // Fetch live information from the HelloWorld example
  // Live info includes the address of an component’s container host and the container’s debug port,
  // formatted in JSON
  programClient.getLiveInfo("HelloWorld", ProgramType.SERVICE, "greet");

  // Fetch program logs in the WordCount example
  programClient.getProgramLogs("WordCount", ProgramType.SERVICE, "RetrieveCounts", 0,
                               Long.MAX_VALUE);

  // Scale a service in the HelloWorld example
  programClient.setServiceInstances("HelloWorld", "greet", 3);

  // Stop a service in the HelloWorld example
  programClient.stop("HelloWorld", ProgramType.SERVICE, "greet");

  // Start, scale, and stop a flow in the WordCount example
  programClient.start("WordCount", ProgramType.FLOW, "WordCountFlow");

  // Fetch flow history in the WordCount example
  programClient.getProgramHistory("WordCount", ProgramType.FLOW, "WordCountFlow");

  // Scale a flowlet in the WordCount example
  programClient.setFlowletInstances("WordCount", "WordCountFlow", "Tokenizer", 3);

  // Stop a flow in the WordCount example
  programClient.stop("WordCount", ProgramType.FLOW, "WordCountFlow");


.. _stream-client:

StreamClient
------------
::

  ClientConfig clientConfig;

  // Construct the client used to interact with CDAP
  StreamClient streamClient = new StreamClient(clientConfig);

  // Fetch the stream list
  List streams = streamClient.list();

  // Create a stream, using the Purchase example
  streamClient.create("purchaseStream");

  // Fetch a stream's properties, using the Purchase example
  StreamProperties config = streamClient.getConfig("purchaseStream");

  // Send events to a stream, using the Purchase example
  streamClient.sendEvent("purchaseStream", "Tom bought 5 apples for $10");

  // Read all events from a stream (results in events)
  List<StreamEvent> events = Lists.newArrayList();
  streamClient.getEvents("purchaseStream", 0, Long.MAX_VALUE, Integer.MAX_VALUE, events);

  // Read first 5 events from a stream (results in events)
  List<StreamEvent> events = Lists.newArrayList();
  streamClient.getEvents(streamId, 0, Long.MAX_VALUE, 5, events);

  // Read 2nd and 3rd events from a stream, after first calling getEvents
  long startTime = events.get(1).getTimestamp();
  long endTime = events.get(2).getTimestamp() + 1;
  events.clear()
  streamClient.getEvents(streamId, startTime, endTime, Integer.MAX_VALUE, events);

  //
  // Write asynchronously to a stream
  //
  String streamId = "testAsync";
  List<StreamEvent> events = Lists.newArrayList();

  streamClient.create(streamId);

  // Send 10 async writes
  int msgCount = 10;
  for (int i = 0; i < msgCount; i++) {
    streamClient.asyncSendEvent(streamId, "Testing " + i);
  }

  // Read them back; need to read it multiple times as the writes happen asynchronously
  while (events.size() != msgCount) {
    events.clear();
    streamClient.getEvents(streamId, 0, Long.MAX_VALUE, msgCount, events);
  }

  // Check that there are no more events
  events.clear();
  while (events.isEmpty()) {
    events.clear();
    streamClient.getEvents(streamId, lastTimestamp + 1, Long.MAX_VALUE, msgCount, events);
  }
  //
  // End write asynchronously
  //


.. _dataset-client:

DatasetClient
-------------
::

  ClientConfig clientConfig;

  // Construct the client used to interact with CDAP
  DatasetClient datasetClient = new DatasetClient(clientConfig);

  // Fetch the list of datasets
  List<DatasetSpecification> datasets = datasetClient.list();

  // Create a dataset
  datasetClient.create("someDataset", "someDatasetType");

  // Truncate a dataset
  datasetClient.truncate("someDataset");

  // Delete a dataset
  datasetClient.delete("someDataset");


.. _dataset-module-client:

DatasetModuleClient
-------------------
::

  ClientConfig clientConfig;

  // Construct the client used to interact with CDAP
  DatasetModuleClient datasetModuleClient = new DatasetModuleClient(clientConfig);

  // Add a dataset module
  File moduleJarFile = createAppJarFile(someDatasetModule.class);
  datasetModuleClient("someDatasetModule", SomeDatasetModule.class.getName(), moduleJarFile);

  // Fetch the dataset module information
  DatasetModuleMeta datasetModuleMeta = datasetModuleClient.get("someDatasetModule");

  // Delete all dataset modules
  datasetModuleClient.deleteAll();


.. _dataset-type-client:

DatasetTypeClient
-----------------
::

  ClientConfig clientConfig;

  // Construct the client used to interact with CDAP
  DatasetTypeClient datasetTypeClient = new DatasetTypeClient(clientConfig);

  // Fetch the dataset type information using the type name
  DatasetTypeMeta datasetTypeMeta = datasetTypeClient.get("someDatasetType");

  // Fetch the dataset type information using the classname
  datasetTypeMeta = datasetTypeClient.get(SomeDataset.class.getName());


.. _query-client:

QueryClient
-----------
::

  ClientConfig clientConfig;

  // Construct the client used to interact with CDAP
  QueryClient queryClient = new QueryClient(clientConfig);

  //
  // Perform an ad-hoc query using the Purchase example
  //
  String query = "SELECT * FROM dataset_history WHERE customer IN ('Alice','Bob')"
  QueryHandle queryHandle = queryClient.execute(query);
  QueryStatus status = new QueryStatus(null, false);

  while (QueryStatus.OpStatus.RUNNING == status.getStatus() ||
         QueryStatus.OpStatus.INITIALIZED == status.getStatus() ||
         QueryStatus.OpStatus.PENDING == status.getStatus()) {
    Thread.sleep(1000);
    status = queryClient.getStatus(queryHandle);
  }

  if (status.hasResults()) {
    // Get first 20 results
    List<QueryResult> results = queryClient.getResults(queryHandle, 20);
    // Fetch schema
    List<ColumnDesc> schema = queryClient.getSchema(queryHandle);
    String[] header = new String[schema.size()];
    for (int i = 0; i < header.length; i++) {
      ColumnDesc column = schema.get(i);
      // Hive columns start at 1
      int index = column.getPosition() - 1;
      header[index] = column.getName() + ": " + column.getType();
    }
  }

  queryClient.delete(queryHandle);
  //
  // End perform an ad-hoc query
  //

.. _service-client:

ServiceClient
-------------
::

  ClientConfig clientConfig;

  // Construct the client used to interact with CDAP
  ServiceClient serviceClient = new ServiceClient(clientConfig);

  // Fetch service information using the service in the PurchaseApp example
  ServiceMeta serviceMeta = serviceClient.get("PurchaseApp", "CatalogLookup");


.. _metrics-client:

MetricsClient
-------------
::

  ClientConfig clientConfig;

  // Construct the client used to interact with CDAP
  MetricsClient metricsClient = new MetricsClient(clientConfig);

  // Fetch the total number of events that have been processed by a flow
  JsonObject metric = metricsClient.getMetric("user", "/apps/HelloWorld/flows",
                                              "process.events.processed", "aggregate=true");


.. _monitor-client:

MonitorClient
-------------
::

  ClientConfig clientConfig;

  // Construct the client used to interact with CDAP
  MonitorClient monitorClient = new MonitorClient(clientConfig);

  // Fetch the list of system services
  List<SystemServiceMeta> services = monitorClient.listSystemServices();

  // Fetch status of system transaction service
  String serviceStatus = monitorClient.getSystemServiceStatus("transaction");

  // Fetch the number of instances of the system transaction service
  int systemServiceInstances = monitorClient.getSystemServiceInstances("transaction");

  // Set the number of instances of the system transaction service
  monitorClient.setSystemServiceInstances("transaction", 1);
