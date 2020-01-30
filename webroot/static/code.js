const listContainer = document.querySelector('#service-list');
let servicesRequest = new Request('/service');
fetch(servicesRequest)
.then(function(response) { return response.json(); })
.then(function(serviceList) {
  if (serviceList.length == 0) {
    const noServices = document.createElement("span");
    noServices.innerHTML = 'No services found!';
    listContainer.appendChild(noServices);
  }

  serviceList.forEach(service => {
    const redCircle = document.createElement("span");
    redCircle.className = 'circle-red';

    const greenCircle = document.createElement("span");
    greenCircle.className = 'circle-green';

    const yellowCircle = document.createElement("span");
    yellowCircle.className = 'circle-yellow';

    var tr = document.createElement("tr");
    var col1 = document.createElement("td");
    if (service.status == 'OK') {
        col1.appendChild(greenCircle);
    } else if (service.status === 'UNKNOWN') {
        col1.appendChild(yellowCircle);
    } else if (service.status === 'FAIL') {
        col1.appendChild(redCircle);
    }

    var col2 = document.createElement("td");
    col2.innerHTML = service.name;

    var col3 = document.createElement("td");
    col3.innerHTML = service.url;

    var col4 = document.createElement("td");
    col4.innerHTML = service.date;

    var btn = document.createElement("button");
    btn.innerHTML = 'Delete';
    btn.className = 'delete';
    btn.onclick = function() {
      fetch('/service', {
          method: 'delete',
          headers: {
          'Accept': 'application/json, text/plain, */*',
          'Content-Type': 'application/json'
          },
        body: JSON.stringify({url:service.url})
      }).then(res=> location.reload());
    }

    var col5 = document.createElement("td");
    col5.appendChild(btn);

    tr.appendChild(col1);
    tr.appendChild(col2);
    tr.appendChild(col3);
    tr.appendChild(col4);
    tr.appendChild(col5);
    listContainer.appendChild(tr);
  });
});

const saveButton = document.querySelector('#post-service');
saveButton.onclick = evt => {
    let urlName = document.querySelector('#url-name').value;
    let urlLink = document.querySelector('#url-link').value;

    fetch('/service', {
    method: 'post',
    headers: {
    'Accept': 'application/json, text/plain, */*',
    'Content-Type': 'application/json'
    },
  body: JSON.stringify({name:urlName, url:urlLink})
}).then(res=> location.reload());
}
