angular.module(PKG.name + '.commons')
  .directive('myStartStopButton', function() {
    return {
      restrict: 'E',
      scope: {
        type: '@',
        isStoppable: '@',
        isPreferences: '@',
        preferencesHandler: '&',
        runtimeHandler: '&'
      },
      templateUrl: 'start-stop-button/start-stop-button.html',
      controller: function($scope, $state, MyDataSource, myRuntimeService, myProgramPreferencesService) {
        $scope.isStoppable = ($scope.isStoppable === 'true');

        if ( typeof $scope.isPreferences === 'undefined') {
          $scope.isPreferences = true;
        } else {
          $scope.isPreferences = !!$scope.isPreferences;
        }
        $scope.runtimeArgs = [];
        var path = '/apps/' + $state.params.appId +
                   '/' + $scope.type + '/' + $state.params.programId;
        var dataSrc = new MyDataSource($scope);

        // Poll for status
        dataSrc.poll({
          _cdapNsPath: path + '/status'
        }, function(res) {
          $scope.status = res.status;
        });

        // Do 'action'. (start/stop)
        $scope.do = function(action) {
          var requestObj = {};
          requestObj = {
            _cdapNsPath: path + '/' + action,
            method: 'POST'
          };
          if (action === 'start') {
            $scope.status = 'STARTING';
            if (Object.keys($scope.runtimeArgs).length > 0) {
              requestObj.body = $scope.runtimeArgs;
            }
          } else {
            $scope.status = 'STOPPING';
          }
          dataSrc.request(requestObj)
            .then(function() {
              $state.go($state.current, $state.params, {reload: true});
            });
        };

        // Delegate runtime & preferences handler
        // to the parent of the directive to handle it their own way.
        $scope.openRuntime = function() {
          var fn = $scope.runtimeHandler();
          if ('undefined' !== typeof fn) {
            fn();
          } else {
            myRuntimeService.show().result.then(function(res) {
              $scope.runtimeArgs = res;
            });
          }
        };

        $scope.openPreferences = function() {
          var fn = $scope.preferencesHandler();
          if ('undefined' !== typeof fn) {
            fn();
          } else {
            myProgramPreferencesService.show($scope.type);
          }
        };
      }
    };
  });