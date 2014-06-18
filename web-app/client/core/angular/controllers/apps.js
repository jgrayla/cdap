'use strict';

define(function () {

  /* Items */

  var Ctrl = ['$rootScope', '$scope', '$http', '$routeParams', '$interval',
    function($rootScope, $scope, $http, $routeParams, $interval) {

    $scope.message = "apps";

    var ival = $interval(function() {
      $.get('/apps');
    }, 1000);

    $scope.$on("$destroy", function(){
        clearInterval(ival);
    });


  }];

  return Ctrl;

});