angular.module(PKG.name + '.feature.workflows')
  .controller('WorkflowsRunsStatusController', function($state, $scope, myWorkFlowApi, $filter, $alert, GraphHelpers, MyDataSource, myMapreduceApi) {
    var filterFilter = $filter('filter'),
        params = {
          appId: $state.params.appId,
          workflowId: $state.params.programId,
          scope: $scope
        };

    if ($state.params.runid) {
      var match = filterFilter($scope.RunsController.runs, {runid: $state.params.runid});
      if (match.length) {
        $scope.RunsController.runs.selected = match[0];
      }
    }

    this.data = {};
    myWorkFlowApi.get(params)
      .$promise
      .then(function(res) {
        var edges = [],
            nodes = [],
            nodesFromBackend = angular.copy(res.nodes);

        // Add Start and End nodes as semantically workflow needs to have it.
        nodesFromBackend.unshift({
          type: 'START',
          nodeType: 'ACTION',
          nodeId: '',
          program: {
            programName: 'Start'
          }
        });

        nodesFromBackend.push({
          label: 'end',
          type: 'END',
          nodeType: 'ACTION',
          nodeId: '',
          program: {
            programName: 'End'
          }
        });

        GraphHelpers.expandNodes(nodesFromBackend, nodes);
        GraphHelpers.convertNodesToEdges(angular.copy(nodes), edges);

        nodes = nodes.map(function(item) {
          return angular.extend({
            name: item.program.programName + item.nodeId,
            type: item.nodeType
          }, item);
        });

        this.data = {
          nodes: nodes,
          edges: edges,
          metrics: {},
          current: {},
        };

        var programs = [];
        angular.forEach(res.nodes, function(value) {
          programs.push(value.program);
        });
        this.actions = programs;
      }.bind(this));

    // Still using MyDataSource because the poll needs to be stopped
    var dataSrc = new MyDataSource($scope);

    var path = '/apps/' + $state.params.appId
      + '/workflows/' + $state.params.programId
      + '/runs/' + $scope.RunsController.runs.selected.runid;

    if ($scope.RunsController.runs.length > 0) {

      dataSrc.poll({
        _cdapNsPath: path,
        interval: 1000
      })
      .then(function (response) {

        var pastNodes = Object.keys(response.properties);
        $scope.RunsController.runs.selected.properties = response.properties;

        var activeNodes = filterFilter(this.data.nodes , function(node) {
          return pastNodes.indexOf(node.nodeId) !== -1;
        });

        angular.forEach(activeNodes, function(n) {
          var runid = response.properties[n.nodeId];

          var mapreduceParams = {
            namespace: $state.params.namespace,
            appId: $state.params.appId,
            mapreduceId: n.program.programName,
            runId: runid,
            scope: $scope
          };
          myMapreduceApi.runDetail(mapreduceParams)
            .$promise
            .then(function (result) {
              this.data.current[n.name] = result.status;
            }.bind(this));
        }.bind(this));

        return response;
      }.bind(this))
      .then(function (response) {
        if (response.status === 'COMPLETED' || response.status === 'FAILED') {
          dataSrc.stopPoll(response.__pollId__);
        }
      });
    }


    this.workflowProgramClick = function (instance) {
      if (['START', 'END'].indexOf(instance.type) > -1 ) {
        return;
      }
      if ($scope.RunsController.runs.length) {
        if (instance.program.programType === 'MAPREDUCE' && $scope.RunsController.runs.selected.properties[instance.nodeId]) {
          $state.go('mapreduce.detail.runs.run', {
            programId: instance.program.programName,
            runid: $scope.RunsController.runs.selected.properties[instance.nodeId]
          });
        }
      } else {
        $alert({
          type: 'info',
          content: 'No runs for the workflow: '+ $state.params.programId +' yet.'
        });
      }
    };

    this.stop = function() {
      $alert({
        type: 'info',
        content: 'Stopping a workflow at run level is not possible yet. Will be fixed soon.'
      });
      return;
      // TODO: There is support from backend. We should implement this in UI
      // this.status = 'STOPPING';
      // myWorkFlowApi.stop(params);
    };

  });
