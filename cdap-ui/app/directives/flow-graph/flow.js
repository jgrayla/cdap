var module = angular.module(PKG.name+'.commons');

var baseDirective = {
  restrict: 'E',
  templateUrl: 'flow-graph/flow.html',
  scope: {
    model: '=',
    click: '&'
  },
  controller: 'myFlowController'
};

module.factory('d3', function ($window) {
  return $window.d3;
});

module.factory('dagreD3', function ($window) {
  return $window.dagreD3;
});

module.controller('myFlowController', function($scope) {
  function update(newVal) {
    if (angular.isObject(newVal) && Object.keys(newVal).length) {
      $scope.render();
    }
  }

  $scope.instanceMap = {};
  $scope.labelMap = {};

  $scope.$watch('model', update, true);

});

module.directive('myFlowGraph', function ($filter, $state, $alert, myStreamService) {
  return angular.extend({
    link: function (scope, elem, attr) {
      scope.render = genericRender.bind(null, scope, $filter);
      scope.parentSelector = attr.parent;

      /**
       * Circle radius for flowlets.
       */
      var flowletCircleRadius = 45;

      // Since names are padded inside of shapes, this needs the same padding to be vertically center aligned.
      /**
       * Inside padding for metric count.
       */
      var metricCountPadding = 5;
      /**
       * Width of stream diagram.
       */
      var streamDiagramWidth = 40;
      /**
       * Height of stream diagram.
       */
      var streamDiagramHeight = 30;

      /**
       * Leaf node variables.
       */
      /**
       * Width of the leaf diagram relative to flowlet circle radius.
       */
      var leafDiagramWidth = flowletCircleRadius * 0.75;
      /**
       * Shrinks or expands height of leaf shape.
       */
      var leafYFactor = 0.9;
      /**
       * Shrinks or expands width of leaf shape.
       */
      var leafXFactor = 1.25;
      /**
       * Overflow of leaf into the flowlet/stream shape.
       */
      var leafBuffer = flowletCircleRadius * 0.2;

      var numberFilter = $filter('myNumber');
      scope.getShapes = function() {
        var shapes = {};
        shapes.flowlet = function(parent, bbox, node) {
          var instances = scope.model.instances[node.elem.__data__] || 0;

          // Pushing labels down
          parent.select('.label')
            .attr('transform', 'translate(0,'+ bbox.height / 3 + ')');

          var shapeSvg = parent.insert('circle', ':first-child')
            .attr('x', -bbox.width / 2)
            .attr('y', -bbox.height / 2)
            .attr('r', flowletCircleRadius)
            .attr('class', 'flow-shapes foundation-shape flowlet-svg');

          parent.insert('text')
            .attr('y', -bbox.height/4)
            .text('x' + instances)
            .attr('class', 'flow-shapes flowlet-instance-count');

          var leafOptions = {
            classNames: ['flowlet-events'],
            circleRadius: flowletCircleRadius,
            diagramWidth: leafDiagramWidth
          };
          drawLeafShape(parent, leafOptions);

          parent.insert('text')
            .attr('x', calculateLeafBuffer(parent, leafOptions))
            .attr('y', metricCountPadding)
            .text(numberFilter(scope.model.metrics[scope.labelMap[node.label].name]))
            .attr('class', 'flow-shapes flowlet-event-count');

          node.intersect = function(point) {
            return dagreD3.intersect.circle(node, flowletCircleRadius, point);
          };

          return shapeSvg;
        };

        shapes.stream = function(parent, bbox, node) {
          var w = bbox.width,
          h = bbox.height/2,
          points = [
            { x:   -streamDiagramWidth, y: streamDiagramHeight}, //e
            { x:   -streamDiagramWidth, y: -h - streamDiagramHeight}, //a
            { x:   w/2, y: -h - streamDiagramHeight}, //b
            { x: w, y: -h/2}, //c
            { x: w/2, y: streamDiagramHeight} //d
          ],
          shapeSvg = parent.insert('polygon', ':first-child')
            .attr('points', points.map(function(d) { return d.x + ',' + d.y; }).join(' '))
            .attr('transform', 'translate(' + (-w/8) + ',' + (h * 1/2) + ')')
            .attr('class', 'flow-shapes foundation-shape stream-svg');

          var leafOptions = {
            classNames: ['stream-events'],
            circleRadius: flowletCircleRadius,
            diagramWidth: leafDiagramWidth
          };
          drawLeafShape(parent, leafOptions);

          parent.append('text')
            .attr('x', calculateLeafBuffer(parent, leafOptions))
            .attr('y', metricCountPadding)
            .text(numberFilter(scope.model.metrics[scope.labelMap[node.label].name]))
            .attr('class', 'flow-shapes stream-event-count');

          node.intersect = function(point) {
            return dagreD3.intersect.polygon(node, points, point);
          };

          return shapeSvg;
        };

        return shapes;
      };

      scope.getShape = function(name) {
        var shapeName;

        switch(name) {
          case 'STREAM':
            shapeName = 'stream';
            break;
          default:
            shapeName = 'flowlet';
            break;
        }
        return shapeName;
      };

      scope.handleNodeClick = function(nodeId) {
        scope.handleHideTip(nodeId);
        var instance = scope.instanceMap[nodeId];
        if (instance.type === 'STREAM') {
          myStreamService.show(nodeId);
        } else {
          // $state.go('flows.detail.flowlets.flowlet', { flowletid: nodeId });

          scope.$apply(function(scope) {
            var fn = scope.click();
            if ('undefined' !== typeof fn) {
              fn(nodeId);
            }
          });
        }
      };

      scope.handleTooltip = function(tip, nodeId) {
        tip
          .html(function() {
            return '<strong>' + nodeId + '</strong>';
          })
          .show();
      };

      /**
       * Draws a leaf shape and positions it next to the parent svg.
       */
      function drawLeafShape(svgParent, properties) {
        var diagramWidth = leafDiagramWidth;
        var yFactor = leafYFactor;
        var xFactor = leafXFactor;
        var circleRadius = flowletCircleRadius;
        var classNamesStr = 'flow-shapes leaf-shape';

        if (properties && Object.prototype.toString.call(properties) === '[object Object]') {
          diagramWidth = properties.diagramWidth || diagramWidth;
          yFactor = properties.yFactor || yFactor;
          xFactor = properties.xFactor || xFactor;
          circleRadius = properties.circleRadius || circleRadius;
          if (angular.isArray(properties.classNames)) {
            var classNames = properties.classNames.join(' ');
            classNamesStr = classNames ? 'flow-shapes leaf-shape ' + classNames : 'flow-shapes leaf-shape';
          }
        }

        var pathinfo = [
          {x: 0, y: 0},
          {x: diagramWidth * xFactor, y: -diagramWidth * yFactor},
          {x: diagramWidth * 2, y: 0},
          {x: diagramWidth * xFactor, y: diagramWidth * yFactor},
          {x: 0, y: 0}
        ];

        var line = d3.svg.line()
          .x(function(d){return d.x;})
          .y(function(d){return d.y;})
          // Must use basis interpolation for curve.
          .interpolate("basis-closed");

        svgParent.insert("svg:path")
          .attr("d", line(pathinfo))
          .attr('class', classNamesStr)
          .attr("transform", function() {
            return "translate("
              + (- circleRadius + leafBuffer) + ", 0) rotate(-180)";
          });
      }

      /**
       * Calcualtes where event count should be placed relative to leaf shape and centers it.
       */
      function calculateLeafBuffer(parent, nodeOptions) {
        var w = parent.select(".leaf-shape").node().getBBox().width;
        return - nodeOptions.circleRadius - w / 2 + leafBuffer / 2;
      }

    }
  }, baseDirective);
});

module.directive('myWorkflowGraph', function ($filter) {
  return angular.extend({
    link: function (scope) {
      scope.render = genericRender.bind(null, scope, $filter);
      var defaultRadius = 50;
      scope.getShapes = function() {
        var shapes = {};
        shapes.job = function(parent, bbox, node) {

          // Creating Hexagon
          var xPoint = defaultRadius * 7/8;
          var yPoint = defaultRadius * 1/2;
          var points = [
            // points are listed from top and going clockwise
            { x: 0, y: defaultRadius},
            { x: xPoint, y: yPoint},
            { x: xPoint, y: -yPoint },
            { x: 0, y: -defaultRadius},
            { x: -xPoint, y: -yPoint},
            { x: -xPoint, y: yPoint}
          ];
          var shapeSvg = parent.insert('polygon', ':first-child')
              .attr('points', points.map(function(p) { return p.x + ',' + p.y; }).join(' '));

          switch(scope.model.current[node.elem.__data__]) {
            case 'COMPLETED':
              shapeSvg.attr('class', 'workflow-shapes foundation-shape job-svg completed');
              break;
            case 'RUNNING':
              shapeSvg.attr('class', 'workflow-shapes foundation-shape job-svg running');
              break;
            case 'FAILED':
              shapeSvg.attr('class', 'workflow-shapes foundation-shape job-svg failed');
              break;
            default:
              shapeSvg.attr('class', 'workflow-shapes foundation-shape job-svg');
          }

          node.intersect = function(point) {
            return dagreD3.intersect.polygon(node, points, point);
          };

          return shapeSvg;
        };

        shapes.start = function(parent, bbox, node) {
          var w = bbox.width;
          var points = [
            // draw a triangle facing right
            { x: -30, y: -40},
            { x: 30, y: 0},
            { x: -30, y: 40},
          ];
          var shapeSvg = parent.insert('polygon', ':first-child')
            .attr('points', points.map(function(p) { return p.x + ',' + p.y; }).join(' '))
            .attr('transform', 'translate(' + (w/6) + ')')
            .attr('class', 'workflow-shapes foundation-shape start-svg');

          node.intersect = function(point) {
            return dagreD3.intersect.polygon(node, points, point);
          };

          return shapeSvg;
        };

        shapes.end = function(parent, bbox, node) {
          var w = bbox.width;
          var points = [
            // draw a triangle facing right
            { x: -30, y: 0},
            { x: 30, y: 40},
            { x: 30, y: -40},
          ];
          var shapeSvg = parent.insert('polygon', ':first-child')
            .attr('points', points.map(function(p) { return p.x + ',' + p.y; }).join(' '))
            .attr('transform', 'translate(' + (-w/6) + ')')
            .attr('class', 'workflow-shapes foundation-shape end-svg');

          node.intersect = function(point) {
            return dagreD3.intersect.polygon(node, points, point);
          };

          return shapeSvg;
        };

        shapes.conditional = function(parent, bbox, node) {
          var points = [
            // draw a diamond
            { x:  0, y: -defaultRadius*3/4 },
            { x: -defaultRadius*3/4, y:  0 },
            { x:  0, y:  defaultRadius*3/4 },
            { x:  defaultRadius*3/4, y:  0 }
          ],
          shapeSvg = parent.insert('polygon', ':first-child')
            .attr('points', points.map(function(p) { return p.x + ',' + p.y; }).join(' '))
            .attr('class', 'workflow-shapes foundation-shape conditional-svg');

          node.intersect = function(p) {
            return dagreD3.intersect.polygon(node, points, p);
          };
          return shapeSvg;
        };

        return shapes;
      };

      scope.getShape = function(name) {
        var shapeName;

        switch(name) {
          case 'ACTION':
            shapeName = 'job';
            break;
          case 'FORKNODE':
            shapeName = 'circle';
            break;
          case 'JOINNODE':
            shapeName = 'circle';
            break;
          case 'START':
            shapeName = 'start';
            break;
          case 'END':
            shapeName = 'end';
            break;
          default:
            shapeName = 'conditional';
            break;
        }
        return shapeName;
      };

      scope.handleNodeClick = function(nodeId) {

        scope.handleHideTip(nodeId);
        var instance = scope.instanceMap[nodeId];
        scope.$apply(function(scope) {
          var fn = scope.click();
          if ('undefined' !== typeof fn) {
            fn(instance);
          }
        });
      };

      scope.handleTooltip = function(tip, nodeId) {
        if (['Start', 'End'].indexOf(nodeId) === -1) {
          tip
            .html(function() {
              return '<strong>'+ scope.instanceMap[nodeId].nodeId + ' : ' + scope.instanceMap[nodeId].program.programName +'</strong>';
            })
            .show();
        }

      };
    }
  }, baseDirective);
});

function genericRender(scope) {
  var nodes = scope.model.nodes;
  var edges = scope.model.edges;

  var renderer = new dagreD3.render();
  var g = new dagreD3.graphlib.Graph();

  g.setGraph({
    nodesep: 60,
    ranksep: 100,
    rankdir: 'LR',
    marginx: 30,
    marginy: 30
  })
    .setDefaultEdgeLabel(function () { return {}; });

  // First set nodes and edges.
  angular.forEach(nodes, function (node) {
    var nodeLabel = "";
    if (node.label && node.label.length) {
      nodeLabel = node.label.length > 8? node.label.substr(0,5) + '...': node.label;
    } else {
      nodeLabel = node.name.length > 8? node.name.substr(0,5) + '...': node.name;
    }
    scope.instanceMap[node.name] = node;
    scope.labelMap[nodeLabel] = node;
    g.setNode(node.name, { shape: scope.getShape(node.type), label: nodeLabel});
  });

  angular.forEach(edges, function (edge) {
    g.setEdge(edge.sourceName, edge.targetName);
  });

  angular.extend(renderer.shapes(), scope.getShapes());
  var selector = '';
  // Making the query to be more specific instead of doing
  // it under the entire DOM. This allows us to draw the diagram
  // in multiple places.
  if (scope.parentSelector) {
    selector += scope.parentSelector;
  }
  selector += ' svg';
  // Set up an SVG group so that we can translate the final graph and tooltip.
  var svg = d3.select(selector).attr('fill', 'white');
  var svgGroup = d3.select(selector + ' g');
  var tip = d3.tip()
    .attr('class', 'd3-tip')
    .offset([-10, 0]);
  svg.call(tip);

  // Set up zoom support
  var zoom = d3.behavior.zoom().scaleExtent([0.1, 2]);
  zoom.on('zoom', function() {
    var t = zoom.translate(),
        tx = t[0],
        ty = t[1];
    var scale = d3.event.scale;
    scale = Math.min(2, scale);

    tx = Math.max(tx, (-g.graph().width+100)*scale );
    tx = Math.min(tx, svg.width() - 100);

    ty = Math.max(ty, -g.graph().height*scale);
    ty = Math.min(ty, (g.graph().height));

    var arr = [tx, ty];

    zoom.translate(arr);
    svgGroup.attr('transform', 'translate(' + arr + ') ' +
                                'scale(' + d3.event.scale + ')');
  });
  svg.call(zoom);

  // Run the renderer. This is what draws the final graph.
  renderer(d3.select(selector + ' g'), g);

  /**
   * Handles showing tooltip on mouseover of node name.
   */
  scope.handleShowTip = scope.handleTooltip.bind(null, tip);

  /**
   * Handles hiding tooltip on mouseout of node name.
   */
  scope.handleHideTip = function() {
    tip.hide();
  };

  // Set up onclick after rendering.
  svg
    .selectAll('g.node')
    .on('click', scope.handleNodeClick);

  svg
    .selectAll('g.node text')
    .on('mouseover', scope.handleShowTip)
    .on('mouseout', scope.handleHideTip);

  scope.$on('$destroy', scope.handleHideTip);
  // Center svg.
  var initialScale = 1.1;
  var svgWidth = svg.node().getBoundingClientRect().width;
  if (svgWidth - g.graph().width <= 0) {
    zoom.translate([0,0])
        .scale(svg.width()/g.graph().width)
        .event(svg);
    svg.attr('height', g.graph().height * initialScale + 40);
  } else {
    zoom
      .translate([(svgWidth - g.graph().width * initialScale) / 2, 20])
      .scale(initialScale)
      .event(svg);
    svg.attr('height', g.graph().height * initialScale + 40);

  }
}
