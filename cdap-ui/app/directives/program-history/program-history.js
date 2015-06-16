angular.module(PKG.name + '.commons')
  .directive('myProgramHistory', function() {
    return {
      restrict: 'EA',
      scope: {
        model: '=runs',
        type: '@'
      },
      templateUrl: 'program-history/program-history.html',
      controller: function ($scope) {
        $scope.currentPage = 1;
        $scope.$watch('model', function (newVal) {
            if (!angular.isArray(newVal)) {
              return;
            }
            $scope.runs = newVal.map(function (run) {
              return angular.extend({
                duration: ( run.end? (run.end - run.start) : 0 )
              }, run);
            });
        });
      }
    };
  });
