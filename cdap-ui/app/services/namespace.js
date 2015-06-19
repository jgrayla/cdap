angular.module(PKG.name + '.services')
  .service('myNamespace', function myNamespace($q, MyDataSource, EventPipe, $rootScope, myAuth, MYAUTH_EVENT) {

    this.namespaceList = [];

    var data = new MyDataSource(),
        queryInProgress = null;


    this.getList = function (force) {
      if (!force && this.namespaceList.length) {
          return $q.when(this.namespaceList);
      }

      if (!queryInProgress) {

        queryInProgress = $q.defer();

        data.request(
          {
            _cdapPath: '/namespaces',
            method: 'GET'
          })
            .then(
              (function(res) {

                if(!res.length) {
                  res = [{
                    id: 'default',
                    name: 'default'
                  }];
                }

                this.namespaceList = res;
                queryInProgress.resolve(res);
                queryInProgress = null;
              }).bind(this),
              function (err) {
                queryInProgress.reject(err);
                queryInProgress = null;
                EventPipe.emit('backendDown', 'Problem accessing namespace');
              }
        );

      }

      return queryInProgress.promise;
    };

    this.getDisplayName = function(name) {
      var ns = this.namespaceList.filter(function(namespace) {
        return namespace.name === name;
      });
      return ns[0].name || name;
    };

  });
