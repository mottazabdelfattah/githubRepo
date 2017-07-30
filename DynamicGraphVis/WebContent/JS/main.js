/**
 * 
 */
// main functions

function getGraphSegments(startX, segWidth, startY, segHeight, canvasWidth){
	var graphSegList = [];
	var noSegments = Math.ceil(canvasWidth/segWidth);
	for (i = 0; i < noSegments; i++) {
		graphSegList[i] = new imgSegmentObj(startX,segWidth,startY,segHeight);
		startX +=  segWidth;
	}
	
	return graphSegList;
}


function drawTimeAxis(canvas_scale, imgWidth, pixelTimeGap, isLabeled){
	s=0;
	var ctx_scale = canvas_scale.getContext('2d');
	var canvasScaleHeight = canvas_scale.height
	ctx_scale.font = "10px Arial";
	var id; var d;
	for (i = 0; i < imgWidth; i++) {	// loop over returned pixels from ajax request
		id = ctx_scale.getImageData(i,0,1,1); // only do this once per page
		d  = id.data;                        // only do this once per page
		
		d[0]   = 0;
		d[1]   = 0;
		d[2]   = 0;
		d[3]   = 255;
		ctx_scale.putImageData(id, i, 0 );
		if(i%pixelTimeGap == 0){
			if(isLabeled){
				var tickLabel = i/imgWidth;
				ctx_scale.fillText(tickLabel,i+2,10);
			}
			for(j=1;j<canvasScaleHeight;j++){
				id = ctx_scale.getImageData(i,j,1,1); // only do this once per page
				d  = id.data;                        // only do this once per page
				
				d[0]   = 0;
				d[1]   = 0;
				d[2]   = 0;
				d[3]   = 255;
				ctx_scale.putImageData(id, i, j );
				
			}
		}
		s++;
		
	}
	
	
}

function refreshTheScene(){
	
	var rootGraphObj = graphDict[rootGraphId];
	var queue = [];
	queue.push(rootGraphObj); 
	
	var gObj = null;
	while(queue.length>0){
		gObj = queue.shift();

		if(gObj.refresh != 0){
			var parentSelectionWidth = 0;
			var parentSelectionObj = getParentSelectionObj(gObj);
			
			if(parentSelectionObj != null){
				parentSelectionWidth = parentSelectionObj.width;
				drawComparisonWithLeft(gObj);
			}
			
			drawVertexList(gObj, true);
		}
		
		if(gObj.refresh == 1){
			var segList = gObj.segmentsList;
			var startXHeirarchy = getSelectionHierarchy(segList[0].startX, gObj);
			
			for(var j=0; j<segList.length; j++){
				startXHeirarchy[0] = segList[j].startX;	//replace the first entry only (each segment in the current level has different start w.r.t parent level)
				getImageSegment(gObj, startXHeirarchy, segList[j].width, parentSelectionWidth, segList[j].startY, segList[j].height);
				
			}
		}else if(gObj.refresh == 2){
			redrawCanvasWithNewWidth(gObj);
		}
		
		gObj.refresh = 0;	//reset refresh prop. to zero
		
		
		// add children to the queue
		for(var i = 0; i < gObj.children.length; i++){
			queue.push(gObj.children[i]);
		}
		
	}
	
	// update guidlines and weight slider
	drawTimeLevelControls();
}


function clearSegment(graphObj, startX, segWidth){
	var canvas = getCanvasElement(graphObj,true);
	var ctx = canvas.getContext('2d');
	ctx.clearRect(startX, 0, segWidth, canvas.height);	
}

function drawSegment(graphObj, startX, segWidth, imgData){
	var img = null;
	var canvas = getCanvasElement(graphObj, true);
	var ctx = canvas.getContext('2d');
	
	img = new Image();
	
	img.onload = function() {
		ctx.clearRect(startX, 0, segWidth, canvas.height);	
        ctx.drawImage(this, startX, 0);
    };
    img.src = "data:image/png;base64," + imgData;
    
}

function getParentBottomY(parentGraph){
	var parentBottomY = parentGraph.htmlCoordinates[1]+parentGraph.height;	
	if(isComparisonObjVisible(parentGraph.timeLevel)){
		parentBottomY += parentGraph.height + 10;
	}
	return parentBottomY;
}

function adjustCanvasPositions(parentGraph){
	var parentWidth = parentGraph.width;
	var parentLeftX = parentGraph.htmlCoordinates[0];
	var parentBottomY = getParentBottomY(parentGraph);
	 
	var childGraphs = parentGraph.children;
	var newCanvasCount = childGraphs.length;
	var widthOfCanvas = Math.floor((parentWidth-(marginSpace*(newCanvasCount-1)))/newCanvasCount);	// new canvas width
	
	
	for(var i=0; i<parentGraph.selectionList.length; i++){
		var gObj = getGraphObj(parentGraph.timeLevel-1, parentGraph.selectionList[i].id, parentGraph.id);
		gObj.width = widthOfCanvas;
		gObj.htmlCoordinates[0] = parentLeftX;
		gObj.htmlCoordinates[1] = parentBottomY+50;
		gObj.refresh = 2;	// reload the same image
		
		// compare with left object
		var compObj = null;
		if(i > 0){
			var gLeftObj = getGraphObj(parentGraph.timeLevel-1, parentGraph.selectionList[i-1].id, parentGraph.id);
			compObj = new CompareObj(gLeftObj.id, gObj.id);
			compObj.height = gObj.height;
			compObj.width = gObj.width
			
			compObj.htmlCoordinates[0] = (gLeftObj.htmlCoordinates[0] + gObj.htmlCoordinates[0]) / 2;
			compObj.htmlCoordinates[1] = gObj.htmlCoordinates[1] + gObj.height + 20;
			
			compObj.buttonCoordinates[0] = gLeftObj.htmlCoordinates[0] + gLeftObj.width + vertexMenuWidth + arrowPointerWidth + 4;
			compObj.buttonCoordinates[1] = (gObj.htmlCoordinates[1] + gObj.htmlCoordinates[1] + gObj.height) / 2;
			
			
		}
		
		if(gObj.compareWithLeftObj != null){
			clearComparisonWithLeft(gObj);
		}
		
		gObj.compareWithLeftObj = compObj;
		parentLeftX+=widthOfCanvas+marginSpace;
	}
	
}

/*
function adjustCanvasHeight(gObj, newHieght){
	gObj.height = newHieght;
	var compObj = gObj.compareWithLeftObj;
	if(compObj != null){
		compObj.height = gObj.height;
		compObj.htmlCoordinates[1] = gObj.htmlCoordinates[1] + gObj.height + 10;
		compObj.buttonCoordinates[1] = (gObj.htmlCoordinates[1] + gObj.htmlCoordinates[1] + gObj.height) / 2;
	}
	
}
*/

//redraw after resizing canvas
function redrawCanvasWithNewWidth(gObj){
	var canvas =  document.getElementById('canvas'+gObj.id);
	var ctx = canvas.getContext('2d');
	var imgData=ctx.getImageData(0,0, canvas.width,canvas.height);
	ctx.clearRect(0, 0, canvas.width, canvas.height);	
	canvas.width = gObj.width;
	canvas.style.left = gObj.htmlCoordinates[0]+'px';
	canvas.style.top = gObj.htmlCoordinates[1]+'px';
	ctx.putImageData(imgData,0,0,0,0,canvas.width, canvas.height);
}

function drawTimeLevelControls(){
	for (var t in timeLevelsDict) {
		var timeLevel = parseInt(t);
		var graphArr = timeLevelsDict[timeLevel];
		var rootLeftX = graphDict[rootGraphId].htmlCoordinates[0];
		drawEdgeWeightSlider(timeLevel, graphArr, rootLeftX);
		drawClusterHierarchy(timeLevel, graphArr, rootLeftX);
		
		if(graphDict[rootGraphId].timeLevel !== timeLevel)	// not the root level
			drawGuideLines(timeLevel, graphArr, rootLeftX);
	}
	
}

function drawEdgeWeightSlider(timeLevel, graphArr, rootLeftX){
	var sliderDiv = document.getElementById('sliderdiv'+timeLevel);
	
	var divLeftX = rootLeftX + graphArr[0].width + 700; 
	var divTopX = graphArr[0].htmlCoordinates[1];
	
	var parentGraph = null;
	
	if(sliderDiv == null){
		var rangeId = 'range_'+timeLevel;
		sliderDiv = createHTMLElement('sliderdiv'+timeLevel, 'div', '', '', divLeftX, divTopX, 'slider' , 'absolute', '');
		
		var span1 = createHTMLElement('', 'span', '', '', '', '', '' , '', 'block');
		span1.innerHTML = 'Edge weight';
		
		var span2 = createHTMLElement(rangeId+'_value', 'span', '', '', '', '', '' , '', 'block');
		span2.innerHTML = '1';
		
		var inputRange = createHTMLElement(rangeId, 'input', '', '', '', '', '' , '', '');
		inputRange.type = 'range';
		inputRange.setAttribute("orient",'vertical');
		inputRange.value = '1';
		inputRange.min = '1';
		inputRange.max = maxEdgeWeight[timeLevel];
		inputRange.addEventListener('change', function()
			{
				var weight = this.value;
				span2.innerHTML = weight;
				for(var i=0; i<graphArr.length;i++){
					graphArr[i].minEdgeWeight = weight;
					graphArr[i].refresh = 1;
					
					// update selected vertices
					updateSelectedVertices(graphArr[i], null, 0);	// edge weight filter overwrite other filters
					
					refreshTheScene();
				}
			}, 
			false
		);
		
		sliderDiv.appendChild(span1);
		sliderDiv.appendChild(inputRange);
		sliderDiv.appendChild(span2);
		document.getElementById('graphs').appendChild(sliderDiv);
	}
	
	
	sliderDiv.style.top = graphArr[0].htmlCoordinates[1] + 'px';	// adjust top position
	
}

function drawGuideLines(timeLevel, graphArr, rootLeftX){
	var canvas =  document.getElementById('guidelines'+timeLevel);
	var canvasLeftX = rootLeftX;
	if(canvas == null){
		canvas = createNewCanvas('guidelines'+timeLevel, rootCanvasWidth, 48, canvasLeftX, 0);
   		document.getElementById('graphs').appendChild(canvas);
   		
   		
	}
	var ctx = canvas.getContext('2d');
	ctx.clearRect(0, 0, canvas.width, canvas.height);
	
	var parentGraph = null;
	var parentGraphSelection = null;
	var sLeft, sRight, cLeft, cRight;
	
	for(var i=0; i<graphArr.length;i++){
		var gObj = graphArr[i];
		parentGraph = getParentObj(gObj);
		parentGraphSelection = getParentSelectionObj(gObj);
		
		sLeft = parentGraph.htmlCoordinates[0] +  parentGraphSelection.canvasCoordinates[0] - canvasLeftX;
		sRight = parentGraph.htmlCoordinates[0] + parentGraphSelection.canvasCoordinates[0] + parentGraphSelection.width - canvasLeftX;
		
		cLeft = gObj.htmlCoordinates[0] - canvasLeftX;
		cRight = gObj.htmlCoordinates[0] + gObj.width - canvasLeftX;
		
		ctx.beginPath();
		ctx.moveTo(sLeft,0);
		ctx.lineTo(cLeft,canvas.height);
		ctx.lineTo(cRight,canvas.height);
		ctx.lineTo(sRight,0);
		ctx.closePath();
		ctx.globalAlpha = 0.6;
		ctx.fillStyle = selectionColors[gObj.selectionid-1];	// color array start from zero
		ctx.fill();
		
	}
	
	canvas.style.top = getParentBottomY(parentGraph)+'px';	// adjust top position
	
}

function drawClusterHierarchy(timeLevel, graphArr, rootLeftX){
	var canvas =  document.getElementById('clusterhierarchy'+timeLevel);
	var canvasScale = document.getElementById('clusterhierarchyScale'+timeLevel);
	var canvasLeftX = graphArr[0].width +120;
	var canvasTopX = graphArr[0].htmlCoordinates[1];
	var canvasWidth = dendrogramCanvasWidth+10;
	var canvasScaleWidth = dendrogramCanvasWidth;
	var canvasScaleTopX = canvasTopX - 15;
	if(canvas == null){
		canvas = createNewCanvas('clusterhierarchy'+timeLevel, canvasWidth, graphArr[0].height, canvasLeftX, canvasTopX);
   		document.getElementById('graphs').appendChild(canvas);
   		
   		canvasScale = createNewCanvas('clusterhierarchyScale'+timeLevel, canvasScaleWidth, 10, canvasLeftX, canvasScaleTopX);
   		document.getElementById('graphs').appendChild(canvasScale);
	}
	var ctx = canvas.getContext('2d');
	ctx.clearRect(0, 0, canvas.width, canvas.height);
	
	var endX, endY;
	
	var queue = [clusterHierarchy] ;
	var queueParentXCoordinate = [];
	var queueParentYCoordinate = [];
	
	while (queue.length>0) {
        var obj = queue.shift(); 
        var name = $(obj).attr('name');
        var ycoordinate = $(obj).attr('ycoordinate');
        var xcoordinate = $(obj).attr('xcoordinate');
        
        var parentY = ycoordinate; // default works only on root level
        var parentX = canvasWidth; // default works only on root level
        if(queueParentYCoordinate.length > 0){
        	parentY = queueParentYCoordinate.shift();
        	parentX = queueParentXCoordinate.shift();
        }
        
        	
        var children = $(obj).attr('children');

        //console.log(name);
        //console.log(ycoordinate);
        
        ctx.beginPath();
        ctx.moveTo(parentX,parentY);
        ctx.lineTo(parentX,ycoordinate);
        ctx.lineTo(xcoordinate,ycoordinate);
        
        
        if (typeof children !== typeof undefined && children !== false) {
        	data = $.parseJSON(JSON.stringify(children));
        	$.each(data, function(i, item) {
        		queue.push(item);
        		queueParentXCoordinate.push(xcoordinate);
        		queueParentYCoordinate.push(ycoordinate);
        	});
        }else{
        	ctx.lineTo(0,ycoordinate);	// leaf
        }
        
        ctx.stroke();
        
    }
    var pixelTimeGap = canvasScale.width / 5;
	drawTimeAxis(canvasScale, canvasScale.width, pixelTimeGap, true) // draw scale canvas
	
    canvasScale.style.top = canvasScaleTopX + 'px';	// adjust top position
	canvas.style.top = canvasTopX + 'px';	// adjust top position
	
}


function drawComparisonWithLeft(gObj){
	
	var compareBtn = null;
	var btnId = '';
	var compareObj = gObj.compareWithLeftObj;
	
	if(compareObj != null){
		
		// compare button
		compareBtn = getCompareBtn(compareObj, gObj);
		compareBtn.style.left = compareObj.buttonCoordinates[0]+'px';	// center button exactly between two canvas
		compareBtn.style.top = compareObj.buttonCoordinates[1]+'px';
		
		
		// compare canvas
		var diffCanvas = getCanvasElement(compareObj,false);
		diffCanvas.style.left = compareObj.htmlCoordinates[0] + 'px';
		diffCanvas.style.top = compareObj.htmlCoordinates[1] + 'px';
		diffCanvas.width = compareObj.width;
		diffCanvas.height = compareObj.height;
		
		if(compareObj.isVisible){
			diffCanvas.style.display = 'block';
			computeDifferanceMap(compareObj);
			compareObj.vertexList = getDifferanceVertexList(compareObj);
			drawVertexList(compareObj, false);
			
		}else{
			diffCanvas.style.display = 'none';
			hideDifferanceVertexList(compareObj);
			hideZommedDifferanceVertexList(compareObj);
		}
	}
	
}
	
function clearComparisonWithLeft(gObj){
	var compareObj = gObj.compareWithLeftObj;
	var diffCanvas = getCanvasElement(compareObj,false);
	var compareBtn = getCompareBtn(compareObj, null);
	
	if(diffCanvas != null){
		diffCanvas.parentNode.removeChild(diffCanvas);
		
		var visibilityBefore = isComparisonObjVisible(gObj.timeLevel);
		compareObj.isVisible = false;
		var visibilityAfter = isComparisonObjVisible(gObj.timeLevel);
		if(visibilityBefore == true && visibilityAfter == false)
			shiftYCoordinates(gObj, true);
		
	}
	
	if(compareBtn != null){
		compareBtn.parentNode.removeChild(compareBtn);
	}
	
}

function hideDifferanceVertexList(gObj){
	
	var edgeCountDiv = getEdgeCountDiv(gObj, false, '');
	if(edgeCountDiv != null)
		edgeCountDiv.style.display = 'none';
	
	var verticesDiv = getVerticesDiv(gObj, false, '');
	if(verticesDiv != null)
		verticesDiv.style.display = 'none';
	
	
}

function hideZommedDifferanceVertexList(gObj){
	
	var arrow = getArrow(gObj, false, '');
	if(arrow != null)
		arrow.style.display = 'none';
	
	var menuDiv = getVerticesMenuDiv(gObj, false, '', null, null);
	if(menuDiv != null)
		menuDiv.style.display = 'none';
}

function getDifferanceVertexList(compareObj){
	var compareVertexList = [];
	var lGraph = graphDict[compareObj.leftId];	// left graph
	var rGraph = graphDict[compareObj.rightId];	// right graph
	var rightVertex, leftVertex, leftInEdgeCount, leftOutEdgeCount, rightInEdgeCount, rightOutEdgeCount, pair;
	for(var i=0; i<vertexList.length; i++){	// loop over global (unfiltered) list
		pair = vertexList[i].split('|');
		leftInEdgeCount = 0, leftOutEdgeCount = 0, rightInEdgeCount = 0, rightOutEdgeCount = 0;
		var leftIndex = listContainsId(lGraph.vertexList, pair[0]);
		var rightIndex = listContainsId(rGraph.vertexList, pair[0]);
		
		if(leftIndex != -1 || rightIndex != -1){
			if(leftIndex != -1){
				leftVertex = lGraph.vertexList[leftIndex];
				leftInEdgeCount = leftVertex.inEdgesCount;
				leftOutEdgeCount = leftVertex.outEdgesCount;
			}
			
			if(rightIndex != -1){
				rightVertex = rGraph.vertexList[rightIndex];
				rightInEdgeCount = rightVertex.inEdgesCount;
				rightOutEdgeCount = rightVertex.outEdgesCount;
			}
			
			var diffVertex = new VertexObj(pair[0]);
			diffVertex.inEdgesCount = Math.abs(leftInEdgeCount - rightInEdgeCount);
			diffVertex.outEdgesCount = Math.abs(leftOutEdgeCount - rightOutEdgeCount);
			compareVertexList.push(diffVertex);
		}
		
	}
	
	return compareVertexList;
	
}

function drawVertexList(gObj, editable){
	
	var menuWidth = 150;
	var refreshDivHeight = 20;
	var left = gObj.htmlCoordinates[0] + gObj.width;
	
	var menuDiv, zommedVerticesDiv, refreshBtnDiv, arrow, selectAllChk, edgeCountDiv;
	var verticesDiv = getVerticesDiv(gObj, false, '');
	
	if(verticesDiv == null){	// create
		
		verticesDiv = getVerticesDiv(gObj, true, '');					// dash's div
		edgeCountDiv = getEdgeCountDiv(gObj, true, vertexMenuWidth);	// create div that holds the information about edges count per vertex
		arrow = getArrow(gObj, true, '');								// the pointer-arrow
		selectAllChk = getSelectAllChk(gObj, true, '');
		refreshBtnDiv = getRefreshBtnDiv(gObj, true, menuWidth, selectAllChk, editable);	// create refresh div
		zommedVerticesDiv = getZommedVerticesDiv(gObj, true, menuWidth);				// the pop-up menu with vertices names
		menuDiv = getVerticesMenuDiv(gObj, true, menuWidth, refreshBtnDiv, zommedVerticesDiv);	// container div
		
		registerRefreshBtnDiv(gObj, refreshBtnDiv, zommedVerticesDiv);	// register refresh buttons click events
		
	}else{	// already exists
		
		edgeCountDiv = getEdgeCountDiv(gObj, false, '');	// get div that holds the information about edges count per vertex
		arrow = getArrow(gObj, false, '');								// the pointer-arrow
		selectAllChk = getSelectAllChk(gObj, false, '');
		refreshBtnDiv = getRefreshBtnDiv(gObj, false, '', null, false);	// get refresh div
		zommedVerticesDiv = getZommedVerticesDiv(gObj, false, '');				// the pop-up menu with vertices names
		menuDiv = getVerticesMenuDiv(gObj, false, '', null, null);	// container div
	}
	
	verticesDiv.style.display = 'block';
	verticesDiv.style.left = left + arrowPointerWidth + 'px';
	verticesDiv.style.top = gObj.htmlCoordinates[1]+'px';
	
	menuDiv.style.left = left + vertexMenuWidth + arrowPointerWidth + 'px';
	menuDiv.style.top = gObj.htmlCoordinates[1] - refreshDivHeight+'px';
	
	arrow.style.left = left+'px';
	
	edgeCountDiv.style.left = left + 'px';
	edgeCountDiv.style.top = gObj.htmlCoordinates[1] - refreshDivHeight+'px';
	
	// clear first
	while (verticesDiv.firstChild) {
		verticesDiv.removeChild(verticesDiv.firstChild);
		zommedVerticesDiv.removeChild(zommedVerticesDiv.firstChild);		
	}
	
	// draw vertices
	var p_ele, p_ele_zoomed, checkbox, chk_label, pair, refreshBtn, countChecked=0;
	var p_ele_in_edges, p_ele_out_edges, p_ele_div;
	var oldClusterId = "";
	for(var i=0; i<vertexList.length; i++){	// loop over global (unfiltered) list
		pair = vertexList[i].split('|');
		p_ele_div = createHTMLElement('', 'div', '', pixelVerticalGap, '', '', '' , '', '');
		p_ele_div.style.lineHeight = pixelVerticalGap+'px';
		p_ele_div.style.textAlign='center';
		
		p_ele = createHTMLElement('', 'p', 5, pixelVerticalGap, '', '', 'vertexDash' , '', 'inline-block');
		
		p_ele_in_edges = createHTMLElement('', 'p', maxVertexBarLength, '', '', '', 'vertexInEdge' , '', 'inline-block');
		
		p_ele_out_edges = createHTMLElement('', 'p', maxVertexBarLength, '', '', '', 'vertexOutEdge' , '', 'inline-block');
		
		p_ele.style.backgroundColor = '#cecece';
		
		//p_ele_out_edges.style.backgroundColor = 'blue';
		//p_ele_in_edges.style.backgroundColor = 'green';
		
		//var chk_id = 'chk_'+pair[0]+'_'+zommedVerticesDiv.id;
		checkbox = createHTMLElement('', 'input', '', '', '', '', '' , '', '');
		checkbox.type = 'checkbox';
		checkbox.value = pair[0];
		if(!editable)
			checkbox.disabled = true;
		
		var vertIndex = listContainsId(gObj.vertexList, pair[0]);
		// is vertex included
		if(vertIndex != -1){
			checkbox.checked = 'true';
			countChecked++;
			
			var vertex = gObj.vertexList[vertIndex];
			if(maxInEdgeCount > 0){
				var w_in = ( Math.log(vertex.inEdgesCount) / Math.log(maxInEdgeCount) ) * 100;
				//p_ele_in_edges.style.width = w_in+'px';
				p_ele_in_edges.style.backgroundImage = getCssValuePrefix() +'linear-gradient(left, green, green '+w_in+'%, transparent '+w_in+'%, transparent 100%)';
				p_ele_in_edges.style.height = pixelVerticalGap+'px';
				p_ele_in_edges.title = vertex.inEdgesCount+'';
				
				var w_out = ( Math.log(vertex.outEdgesCount) / Math.log(maxOutEdgeCount) ) * 100;
				//p_ele_out_edges.style.width = w_out+'px';
				p_ele_out_edges.style.backgroundImage = getCssValuePrefix() +'linear-gradient(right, blue, blue '+w_out+'%, transparent '+w_out+'%, transparent 100%)';
				p_ele_out_edges.style.height = pixelVerticalGap+'px';
				p_ele_out_edges.title = vertex.outEdgesCount+'';
			}
			
		}
		p_ele_div.appendChild(p_ele_out_edges);
		p_ele_div.appendChild(p_ele);
		p_ele_div.appendChild(p_ele_in_edges);
		verticesDiv.appendChild(p_ele_div);
		
		
		chk_label = createHTMLElement('', 'label', menuWidth, '', '', '', '' , '', '');
		var text = pair[1];
		if(text.length > 15)
			text = text.substring(0,15)+' ..';	// take first 15 characters
		chk_label.innerHTML = text;
		
		p_ele_zoomed = createHTMLElement('', 'p', '', '', '', '', '' , '', '');
		p_ele_zoomed.title = pair[1];
		
		
		p_ele_zoomed.appendChild(checkbox);
		p_ele_zoomed.appendChild(chk_label);
		
		zommedVerticesDiv.appendChild(p_ele_zoomed);
	}
	
	if(countChecked == vertexList.length && editable)
		selectAllChk.checked = 'true';
	
	document.getElementById('graphs').appendChild(menuDiv);
	registerMouseHover(gObj, verticesDiv, zommedVerticesDiv, menuDiv, arrow, edgeCountDiv);
}

function toggleCompareCanvas(gObjID){
	var gObj = graphDict[gObjID];
	
	var visibilityBefore = isComparisonObjVisible(gObj.timeLevel);
	
	gObj.compareWithLeftObj.isVisible = !gObj.compareWithLeftObj.isVisible;
	
	var visibilityAfter = isComparisonObjVisible(gObj.timeLevel);
	
	if(visibilityBefore == true && visibilityAfter == false){
		shiftYCoordinates(gObj, true);
	}else if(visibilityBefore == false && visibilityAfter == true){
		shiftYCoordinates(gObj,false);
	}
	gObj.refresh = 2;
	refreshTheScene();
}


function shiftYCoordinates(gObj, up){
	var timeLevel = gObj.timeLevel - 1;
	var graphObjects = null;
	var value = gObj.height + 10; 
	
	if(up)	// if shift up
		value*=-1;
	
	for(var t = timeLevel; t >= 0; t--){
		
		graphObjects = timeLevelsDict[t];
		
		if(graphObjects !=null){
			for(var i=0; i<graphObjects.length; i++){
				gObj = graphObjects[i];
				
				gObj.htmlCoordinates[1] += value;
				if(gObj.compareWithLeftObj != null){
					gObj.compareWithLeftObj.htmlCoordinates[1] += value;
					gObj.compareWithLeftObj.buttonCoordinates[1] += value;
				}
				
				for(var j=0; j<gObj.selectionList.length; j++){
					gObj.selectionList[j].canvasCoordinates[1] += value;
				}
				gObj.refresh = 2;
			}
		} 
		
	}
	
	
}

// return false if ALL comp. objects are invisible and true if at least one obj is visible
function isComparisonObjVisible(timeLevel){
	var graphObjects = timeLevelsDict[timeLevel];
	var isVisible = false;
	for(var i=0; i<graphObjects.length; i++){
		if(graphObjects[i].compareWithLeftObj != null && graphObjects[i].compareWithLeftObj.isVisible){
			isVisible = true;
			break;
		}
	}
	return isVisible;
}


function computeDifferanceMap(compareObj){
	var comparecanvas = getCanvasElement(compareObj, false);
	var can1 = getCanvasElement(graphDict[compareObj.leftId], false);
	var can2 = getCanvasElement(graphDict[compareObj.rightId], false);
	
	var compare_ctx = comparecanvas.getContext('2d');
	var can1_ctx = can1.getContext('2d');
	var can2_ctx = can2.getContext('2d'); 
	
  	
	var diffImageData = compare_ctx.getImageData(0, 0, comparecanvas.width, comparecanvas.height);
	var image1Data = can1_ctx.getImageData(0, 0, can1.width, can1.height);
	var image2Data = can2_ctx.getImageData(0, 0, can2.width, can2.height);
	
	var diffData = diffImageData.data;
	var data1 = image1Data.data;	
  	var data2 = image2Data.data;
  	
  	// data1 and data2 should be the same size
  	var diff=0;
  	for (var i = 0; i < data1.length; i += 4) {
  		diffData[i] = Math.abs(data1[i] - data2[i]);  // red
  		diffData[i+1]= Math.abs(data1[i+1] - data2[i+1]);	//green
  		diffData[i+2]= Math.abs(data1[i+2] - data2[i+2]);	// blue
  		diffData[i+3]= 255;
   	}
  	//compare_ctx.clearRect(0, 0, comparecanvas.width, comparecanvas.height);	
  	compare_ctx.putImageData(diffImageData,0,0);
  	
}


function getSelectionHierarchy(startX, gObj){
	
	var startXHeirarchy = [];
	var index = -1;
	startXHeirarchy.push(startX);
	var tmpObj = gObj;
	var oldTmpObj = null;
	var selectionObj = null;
	while(tmpObj.parentid !== rootChar){
		
		oldTmpObj = tmpObj;
		tmpObj = graphDict[tmpObj.parentid];
		
		index = listContainsId(tmpObj.selectionList,oldTmpObj.selectionid);
		selectionObj = tmpObj.selectionList[index];
		startXHeirarchy.push(selectionObj.canvasCoordinates[0]);
	}
	
	return startXHeirarchy;
}


function setDateTimeLabelText(label, gObj, startX, isAppend){

	var startXHeirarchy = getSelectionHierarchy(startX, gObj);
    getSelectionDate(label.id, startXHeirarchy, gObj.timeLevel, isAppend);
}


