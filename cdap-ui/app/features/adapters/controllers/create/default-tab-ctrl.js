angular.module(PKG.name + '.feature.adapters')
  .controller('DefaultCanvasController', function($scope, AdapterApiFactory, $q, $filter) {
    var createCtrl = $scope.AdapterCreateController;
    var filterFilter = $filter('filter'),
        capitalizeFilter = $filter('caskCapitalizeFilter'),
        icon,
        match;
    function formatProperties(properties) {
      var defer = $q.defer();
      var obj = {};
      angular.forEach(properties, function(property) {
        obj[property.name] = '';
      });
      defer.resolve(obj);
      return defer.promise;
    };

    this.addSource = function (sourceName) {
      var source = {
        name: sourceName
      };
      AdapterApiFactory
        .fetchSourceProperties(
          {
            scope: $scope,
            adapterType: createCtrl.model.metadata.type,
            source: sourceName
          }
        )
        .$promise
        .then(function(res) {
          var pluginProperties = (res.length? res[0].properties: {});
          source._backendProperties = pluginProperties;
          return formatProperties(pluginProperties);
        })
        .then(function(properties) {
          source.properties = properties;
          createCtrl.model.setSource(source);
        });
    };

    this.addTransform = function(transformName) {
      var transform = {
        name: transformName
      };
      AdapterApiFactory
        .fetchTransformProperties(
          {
            scope: $scope,
            adapterType: createCtrl.model.metadata.type,
            transform: transformName
          }
        )
        .$promise
        .then(function(res) {
          var pluginProperties = (res.length? res[0].properties : {});
          transform._backendProperties = pluginProperties;
          return formatProperties(pluginProperties);
        })
        .then(function(properties) {
          transform.properties = properties;
          createCtrl.model.setTransform([transform]);
        });
    };

    this.addSink = function(sinkName) {
      var sink = {
        name: sinkName
      };
      AdapterApiFactory
        .fetchSinkProperties(
          {scope: $scope, adapterType: createCtrl.model.metadata.type, sink: sinkName})
        .$promise
        .then(function(res) {
          var pluginProperties = (res.length? res[0].properties : {});
          sink._backendProperties = pluginProperties;
          return formatProperties(pluginProperties);
        })
        .then(function(properties) {
          sink.properties = properties;
          createCtrl.model.setSink(sink);
        });
    };

    function editPluginProperties(type, partialPath) {
      if (createCtrl.model[type].placeHolder) {
        return;
      }

      match = filterFilter(createCtrl.tabs, {type: type});
      if (match.length) {
        createCtrl.tabs[
          createCtrl.tabs.indexOf(match[0])
        ].active = true;
      } else {
        icon = filterFilter(
          createCtrl['default' + capitalizeFilter(type) + 's'],
          {name: createCtrl.model[type].name}
        );
        createCtrl.tabs.active = (createCtrl.tabs.push({
          title: createCtrl.model[type].name,
          icon: icon.length && icon[0].icon,
          type: type,
          active: true,
          partial: partialPath
        })) -1;
      }
    }
    this.editSourceProperties = function() {
      editPluginProperties(
        'source',
        '/assets/features/adapters/templates/create/tabs/edit-source-properties.html'
      );
    };

    this.editSinkProperties = function() {
      editPluginProperties(
        'sink',
        '/assets/features/adapters/templates/create/tabs/edit-sink-properties.html'
      );
    };

    this.editTransformProperty = function(transform) {
      if (transform.placeHolder){
        return;
      }

      match = filterFilter(createCtrl.tabs, {
        transformid: transform.$$hashKey,
        type: 'transform'
      });
      if (match.length) {
        createCtrl.tabs[createCtrl.tabs.indexOf(match[0])].active = true;
      } else {
        icon = filterFilter(
          createCtrl.defaultTransforms,
          {name: transform.name}
        );
        createCtrl.tabs.active = (createCtrl.tabs.push({
          title: transform.name,
          icon: icon.length && icon[0].icon,
          transformid: transform.$$hashKey,
          transform: transform,
          active: true,
          type: 'transform',
          partial: '/assets/features/adapters/templates/create/tabs/edit-transform-properties.html'
        })) -1;
      }
    };

    this.deleteTransform = function(transform) {
      var index = createCtrl.model.transforms.indexOf(transform);
      createCtrl.model.transforms.splice(index, 1);
      if (!createCtrl.model.transforms.length) {
        createCtrl.model.transforms.push({
          name: 'Add a Transform',
          placeHolder: true,
          properties: {}
        });
      }
    };

  });
