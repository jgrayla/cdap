'use strict';

define(function () {

  /* Items */

  var Ctrl = ['$rootScope', '$scope', '$http', '$routeParams',
    function($rootScope, $scope, $http, $routeParams) {

    $scope.message = "overview";

  }];

  return Ctrl;

});