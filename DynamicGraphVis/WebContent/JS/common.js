/**
 * 
 */
// define Objects
function GraphObj(selectionid, parentid, timeLevel) {
    this.id = timeLevel+"_"+selectionid+'_'+parentid;
    this.selectionid = selectionid;
    this.parentid = parentid;
    this.timeLevel = timeLevel;
    this.segmentsList = [];
    this.htmlCoordinates = [];	// topleft X and Y positions
    this.width = 0;
    this.height = defaultCanvasHeight;
    this.refresh = 1;		// 0 -> don't refresh, 1 -> reload the image from source, 2 -> reload cropped image, 3 refresh the vertices
    this.minEdgeWeight = 1;
    this.selectionList = [];
    this.children = [];
    this.compareWithLeftObj = null;
    this.vertexList = [];
    this.selectedVertex = -1;
}

function imgSegmentObj(startX,width,startY,height){
	this.startX = startX;
	this.width = width;
	this.startY = startY;
	this.height = height;
}

function CompareObj(leftId, rightId){
	this.id = leftId+'|'+rightId;
	this.leftId =  leftId;
	this.rightId =  rightId;
	//this.segmentsList =  [];
	this.htmlCoordinates = [];	// topleft X and Y positions
	this.buttonCoordinates = [];
	this.width = 0;
    this.height = 0;
	this.isVisible = false;
	this.vertexList = [];
}

function selectionRect(id){
	this.id = id;
	this.canvasCoordinates = [];		// topleft X and Y positions
	this.width = 0;
    this.height = 0;
    this.isVisible = false;
    this.type = 1; // 1 is time (default), 2 is space and time
}

function VertexObj(id){
	this.id = id;
	this.name =  "";
	this.inEdgesCount = 0;
	this.outEdgesCount = 0;
}



// common function
function createHTMLElement(id, type, w, h, l, t, className, position, display){
	// create new canvas
	var element = document.createElement(type);
	if(id != '')
		element.id = id;
	if(position != '')
		element.style.position = position;
	if(className != '')
		element.className = className;
	if(w != '')
		element.style.width = w + 'px';
	if(h != '')
		element.style.height = h + 'px';
	if(l != '')
		element.style.left = l + 'px';
	if(t != '')
		element.style.top = t + 'px';
	if(display !='')
		element.style.display = display;
	
	return element;
}

function createNewCanvas(id, w, h, l, t){
	// create new canvas
	var canvas = document.createElement("canvas");
	canvas.id = id;
	canvas.style.position = 'absolute';
	canvas.width = w;
	canvas.height = h;
	canvas.style.left = l+'px';
	canvas.style.top = t+'px';
	
	return canvas;
}

function listContainsId(list, id){
	for(var i=0;i<list.length;i++){
		if(list[i].id === id){
			return i;
		}
	}
	return -1
}

function getGraphObj(timeLevel, selectionid, parentid){
	var graphId = timeLevel+'_'+selectionid+'_'+parentid;
    return graphDict[graphId];
}

function getParentObj(gObj){
	var parent = null;
	if(gObj.parentid !== rootChar){
		parent = graphDict[gObj.parentid];
	}
	return parent;
}

function getParentSelectionObj(gObj){
	var selectionObj = null;
	var parentGraphObj = getParentObj(gObj);
	if(parentGraphObj != null){
		var index = listContainsId(parentGraphObj.selectionList, gObj.selectionid);
		selectionObj = parentGraphObj.selectionList[index];
	}
	return selectionObj;
}

function updateTimeLevelDictionary(key, value){
	var gObjs = timeLevelsDict[key];
	if(gObjs == null){
		gObjs = [];
		gObjs.push(value);
		timeLevelsDict[key] = gObjs;
	}else{
		gObjs.push(value);
	}
}


function sortSelectionListByStartX(selectionList){
	for(var i=0; i<selectionList.length; i++){
		for(var j=i+1; j<selectionList.length; j++){
			if(selectionList[i].canvasCoordinates[0] > selectionList[j].canvasCoordinates[0]){
				var tmp = selectionList[i];
				selectionList[i] = selectionList[j];
				selectionList[j] = tmp;
			}
		}
	}
}

function getCanvasElement(gObj, registerMouse){
	var canvas =  document.getElementById('canvas'+gObj.id);
	if(canvas == null){
		canvas = createNewCanvas ('canvas'+gObj.id, gObj.width, gObj.height, 0, 0);
   		document.getElementById('graphs').appendChild(canvas);
   		if(registerMouse)
   			registerMouseEvents(canvas, gObj);
	}
	
	canvas.style.left = gObj.htmlCoordinates[0]+'px';
	canvas.style.top = gObj.htmlCoordinates[1]+'px';
	
	return canvas;
}


// if gObj = null, then don't create button it is not exist
function getCompareBtn(compareObj, gObj){
	var btnId = 'compareBtn_' + compareObj.id;
	var compareBtn = document.getElementById(btnId);
	if(compareBtn == null && gObj != null){
		compareBtn = createHTMLElement(btnId, 'a', '', '', '', '', 'compareLink', 'absolute', '');
		compareBtn.href =  "javascript:toggleCompareCanvas('"+gObj.id+"');";  
		compareBtn.innerHTML = "";
		document.getElementById('graphs').appendChild(compareBtn);
	}
	return compareBtn;
}

/********************** functions related to vertex list ****************************/
function getVerticesMenuDiv(gObj, createIfNotExits, width, refreshBtnDiv, zommedVerticesDiv){
	var id = 'verticesMenuDiv'+gObj.id;
	var menuDiv = document.getElementById(id);
	if(menuDiv == null && createIfNotExits){
		menuDiv = createHTMLElement(id, 'div', width, '', '', '', '' , 'absolute', 'none');
		menuDiv.appendChild(refreshBtnDiv);
		menuDiv.appendChild(zommedVerticesDiv);
		menuDiv.style.backgroundColor = 'white';
		menuDiv.style.zIndex = '1';
		menuDiv.style.border = 'solid 1px';
	}
	
	return menuDiv;
}

function getVerticesDiv(gObj, createIfNotExits, width){
	var id = 'verticesDiv'+gObj.id;
	var verticesDiv = document.getElementById(id);
	if(verticesDiv == null && createIfNotExits){
		verticesDiv = createHTMLElement(id, 'div', '', '', '', '', 'vertexlist' , '', '');
		verticesDiv.style.zIndex = '1';
		document.getElementById('graphs').appendChild(verticesDiv);
	}
	
	return verticesDiv;
}

function getEdgeCountDiv(gObj, createIfNotExits, width){
	var id = 'edgeCountDiv'+gObj.id;
	var edgeCountDiv = document.getElementById(id);
	if(edgeCountDiv == null && createIfNotExits){
		edgeCountDiv = createHTMLElement(id, 'div', width, '', '', '', '' , 'absolute', 'none');
		var inEdgeCountLabel = createHTMLElement('', 'p', '', '', '', '', 'inEdgeCount' , '', '');
		var outEdgeCountLabel = createHTMLElement('', 'p', '', '', '', '', 'outEdgeCount' , '', '');
		edgeCountDiv.appendChild(outEdgeCountLabel);
		edgeCountDiv.appendChild(inEdgeCountLabel);
		edgeCountDiv.style.zIndex = '1';
		document.getElementById('graphs').appendChild(edgeCountDiv);
	}
	
	return edgeCountDiv;
}

function getArrow(gObj, createIfNotExits, width){
	var id = 'pointerArrow'+gObj.id;
	var arrow = document.getElementById(id);
	if(arrow == null && createIfNotExits){
		arrow = createHTMLElement(id, 'i', '', '', '', '', 'right-arrow' , '', 'none');
		document.getElementById('graphs').appendChild(arrow);
	}
	
	return arrow;
}

function getSelectAllChk(gObj, createIfNotExits, width){
	var id = 'selectAllChkVertices'+gObj.id;
	var selectAllChk = document.getElementById(id);
	if(selectAllChk == null && createIfNotExits){
		selectAllChk = createHTMLElement(id, 'input', '', '', '', '', '' , '', '');
		selectAllChk.type = 'checkbox';
	}
	
	return selectAllChk;
}

function getZommedVerticesDiv(gObj, createIfNotExits, width){
	var id = 'zoomedVerticesDiv'+gObj.id;
	var zommedVerticesDiv = document.getElementById(id);
	if(zommedVerticesDiv == null && createIfNotExits){
		zommedVerticesDiv = createHTMLElement(id, 'div', width, '', '', '', 'vertexlist-zoom' , '', '');
		zommedVerticesDiv.setAttribute('tabindex','1'); 
		zommedVerticesDiv.style.verticalAlign = "top";
	}
	
	return zommedVerticesDiv;
}

function getRefreshBtnDiv(gObj, createIfNotExits, width, selectAllChk, editable){
	var id = 'refreshBtnDiv'+gObj.id;
	var refreshBtnDiv = document.getElementById(id);
	if(refreshBtnDiv == null && createIfNotExits){
		refreshBtnDiv = createHTMLElement(id, 'div', width, '', '', '', '' , '', '');
		// hide button
		var hideBtn = createHTMLElement('', 'a', '', '', '', '', 'hideLink' , '', '');
		hideBtn.innerHTML = "";
		hideBtn.title = "hide menu";
		//hideBtn.href = "#";
		hideBtn.style.float = 'right';
		
		refreshBtnDiv.appendChild(hideBtn);
		
		if(editable){
			// refresh buttons (inter-relation and intra-relation)
			var refreshBtnInter = createHTMLElement('', 'a', '', '', '', '', 'interLink' , '', '');
			refreshBtnInter.innerHTML = "";
			refreshBtnInter.title = "inter-relation";
			//refreshBtnInter.href = "#";
			refreshBtnInter.style.float = 'right';
			
			var refreshBtnIntra = createHTMLElement('', 'a', '', '', '', '', 'intraLink' , '', '');
			refreshBtnIntra.innerHTML = "";
			refreshBtnIntra.title = "intra-relation";
			//refreshBtnIntra.href = "#";
			refreshBtnIntra.style.float = 'right';
			
			var selectAllLabel = createHTMLElement('', 'label', '', '', '', '', '' , '', '');
			selectAllLabel.innerHTML = "All";
			selectAllLabel.style.margin = '2px';
			
			refreshBtnDiv.appendChild(refreshBtnInter);
			refreshBtnDiv.appendChild(refreshBtnIntra);
			
			refreshBtnDiv.appendChild(selectAllChk);
			refreshBtnDiv.appendChild(selectAllLabel);
		}

	}
	
	return refreshBtnDiv;
}


function translateObjListtoIDList(list){
	var idList = [];
	for(var i = 0; i < list.length; i++){
		idList[i] = list[i].id;
	}
	return idList;
}


function translatePipeSeparatedStringToVertObj(str){
	var entries = str.split('|');
	var vertex = new VertexObj(entries[0]);
	vertex.name = entries[1];
	vertex.inEdgesCount = entries[2];
	vertex.outEdgesCount = entries[3];
	vertex.clusterId = entries[4];
	
	return vertex;
}

function translatePipeSeparatedStringToClusterObj(str){
	var entries = str.split('|');
	var cluster = new VertexCluster(entries[0]);
	cluster.offset = entries[1];
	
	return cluster;
}


function nFormatter(num, digits) {
	  var si = [
	    { value: 1E18, symbol: "E" },
	    { value: 1E15, symbol: "P" },
	    { value: 1E12, symbol: "T" },
	    { value: 1E9,  symbol: "G" },
	    { value: 1E6,  symbol: "M" },
	    { value: 1E3,  symbol: "k" }
	  ], rx = /\.0+$|(\.[0-9]*[1-9])0+$/, i;
	  for (i = 0; i < si.length; i++) {
	    if (num >= si[i].value) {
	      return (num / si[i].value).toFixed(digits).replace(rx, "$1") + si[i].symbol;
	    }
	  }
	  return num;//.toFixed(digits).replace(rx, "$1");
}

function getCssValuePrefix()
{
    var rtrnVal = '';//default to standard syntax
    var prefixes = ['-o-', '-ms-', '-moz-', '-webkit-'];

    // Create a temporary DOM object for testing
    var dom = document.createElement('div');

    for (var i = 0; i < prefixes.length; i++)
    {
        // Attempt to set the style
        dom.style.background = prefixes[i] + 'linear-gradient(#000000, #ffffff)';

        // Detect if the style was successfully set
        if (dom.style.background)
        {
            rtrnVal = prefixes[i];
        }
    }

    dom = null;
    delete dom;

    return rtrnVal;
}


function updateSelectedVertices(gObj, selectedVertexList, actionId){
	var parentSelectionObj = getParentSelectionObj(gObj);
	var response;
	if(parentSelectionObj != null){
		response = filterSelectedVertices(gObj, 0, parentSelectionObj.width, parentSelectionObj.canvasCoordinates[1], parentSelectionObj.height, selectedVertexList, actionId);
	}else{
		response = filterSelectedVertices(gObj, 0, 0, 0, 0, selectedVertexList, actionId);
	}
	
    var vertexList = response["vertexList"];
    gObj.vertexList.length = 0;	// clear
    for(var i=0; i<vertexList.length; i++){
		var vertex = translatePipeSeparatedStringToVertObj(vertexList[i]);
		gObj.vertexList.push(vertex);	
	}
}
