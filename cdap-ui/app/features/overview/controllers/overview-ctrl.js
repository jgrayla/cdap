/**
 * OverviewCtrl
 */

angular.module(PKG.name+'.feature.overview').controller('OverviewCtrl',
function ($scope, $state, myLocalStorage, MY_CONFIG, Widget, MyMetricsQueryHelper, myHelpers, MyChartHelpers, MyDataSource) {

  var dataSrc = new MyDataSource($scope);
  if(!$state.params.namespace) {
    // the controller for "ns" state should handle the case of
    // an empty namespace. but this nested state controller will
    // still be instantiated. avoid making useless api calls.
    return;
  }

  this.hideWelcomeMessage = false;


  var PREFKEY = 'feature.overview.welcomeIsHidden';

  myLocalStorage.get(PREFKEY)
    .then(function (v) {
      this.welcomeIsHidden = v;
    }.bind(this));

  this.hideWelcome = function () {
    myLocalStorage.set(PREFKEY, true);
    this.welcomeIsHidden = true;
  };

  this.isEnterprise = MY_CONFIG.isEnterprise;
  this.systemStatus = '#C9C9D1';
  dataSrc.poll({
    _cdapPath: '/system/services/status',
    interval: 10000
  }, function(res) {
    var serviceStatuses = Object.keys(res).map(function(value) {
      return res[value];
    });
    if (serviceStatuses.indexOf('NOTOK') > -1) {
      this.systemStatus = 'yellow';
    }
    if (serviceStatuses.indexOf('OK') === -1) {
      this.systemStatus = 'red';
    }
    if (serviceStatuses.indexOf('NOTOK') === -1) {
      this.systemStatus = 'green';
    }
  }.bind(this));

  this.wdgts = [];
  // type field is overridden by what is rendered in view because we do not use widget.getPartial()
  var widgetSettings = {
    size: {
      height: 100,
      width: 280
    },
    chartMetadata: {
      showx: false,
      showy: false,
      legend: {
        show: false,
        position: 'inset'
      }
    },
    color: {
      pattern: ['red', '#f4b400']
    },
    isLive: true,
    interval: 60*1000,
    aggregate: 5
  };

  this.wdgts.push(
    new Widget({
      title: 'System',
      type: 'c3-line',
      settings: widgetSettings,
      metric: {
        context: 'namespace.*',
        names: ['system.services.log.error', 'system.services.log.warn'],
        startTime: 'now-3600s',
        endTime: 'now',
        resolution: '1m'
      },
      metricAlias: {
        'system.services.log.error': 'System Errors',
        'system.services.log.warn' : 'System Warnings'
      }
    })
  ,
    new Widget({
      title: 'Applications',
      type: 'c3-line',
      settings: widgetSettings,
      metric: {
        context: 'namespace.' + $state.params.namespace,
        names: ['system.app.log.error', 'system.app.log.warn'],
        startTime: 'now-3600s',
        endTime: 'now',
        resolution: '1m'
      },
      metricAlias: {
        'system.app.log.error': 'Application Errors',
        'system.app.log.warn' : 'Application Warnings'
      }
    })
  );

  // TODO: There should be a better way. Right now the 'sort-of' legend for
  // #of warnings and errors are outside the charts. Thats the reason
  // polling happens in two different places.
  angular.forEach(this.wdgts, function(widget) {
    dataSrc.poll({
      _cdapPath: '/metrics/query',
      method: 'POST',
      body: MyMetricsQueryHelper.constructQuery(
        'qid',
        MyMetricsQueryHelper.contextToTags(widget.metric.context),
        widget.metric
      )
    }, function(res) {

      var processedData = MyChartHelpers.processData(
        res,
        'qid',
        widget.metric.names,
        widget.metric.resolution,
        widget.settings.aggregate
      );
      processedData = MyChartHelpers.c3ifyData(processedData, widget.metric, widget.metricAlias);
      widget.chartData = angular.copy(processedData);
    });
  });

});
