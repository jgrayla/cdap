angular.module(PKG.name + '.feature.workflows')
  .controller('WorkflowsRunsController', function($scope, $state, $filter, rRuns) {
    var fFilter = $filter('filter');
    this.runs = rRuns;

    if ($state.params.runid) {
      var match = fFilter(rRuns, {runid: $state.params.runid});
      if (match.length) {
        this.runs.selected = match[0];
      }
    } else if (rRuns.length) {
      this.runs.selected = rRuns[0];
    } else {
      this.runs.selected = {
        runid: 'No Runs'
      };
    }

    $scope.$watch('runs.selected.runid', function() {
     if ($state.params.runid) {
       return;
     } else {
        if (rRuns.length) {
          this.runs.selected = rRuns[0];
        }
     }
   }.bind(this));

    this.tabs = [{
      title: 'Status',
      template: '/assets/features/workflows/templates/tabs/runs/tabs/status.html'
    },
    {
      title: 'Logs',
      template: '/assets/features/workflows/templates/tabs/runs/tabs/log.html'
    }];

    this.activeTab = this.tabs[0];

    this.selectTab = function(tab) {
      this.activeTab = tab;
    };
  });
