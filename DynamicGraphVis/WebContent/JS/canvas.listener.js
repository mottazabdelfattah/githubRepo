function setMousePosition(e) {
    var ev = e || window.event; //Moz || IE
    /*
    if (ev.pageX) { //Moz
        mouse.x = ev.pageX + window.pageXOffset;
        mouse.y = ev.pageY + window.pageYOffset;
    } else if (ev.clientX) { //IE
        mouse.x = ev.clientX + document.body.scrollLeft;
        mouse.y = ev.clientY + document.body.scrollTop;
    }*/
    mouse.x = e.pageX + $(document).scrollLeft();// - canvas.getBoundingClientRect().left;
    mouse.y = e.pageY ;//+ $(document).scrollTop();// - canvas.getBoundingClientRect().top;
};

var mouse = {
    x: 0,
    y: 0,
    startX: 0,
    startY: 0
};

// register mouse events on canvas
function registerMouseEvents(canvas, gObj) {
    var selectionObj = null;

    canvas.onmousemove = function (e) {
    	onMouseMove(e, mouse, selectionObj, gObj);
    }
	
    canvas.onclick = function (e) {
    	selectionObj = null;	//reset reference variable
		var selectionIndex = listContainsId(gObj['selectionList'], currentSelection);	//get selectionObj if exists
		if(selectionIndex != -1){
			selectionObj = gObj['selectionList'][selectionIndex];
		}
        if (selectionObj !== null && !selectionObj.isVisible){
    		canvas.style.cursor = "default";
           
            var childGraphObj = getGraphObj(gObj['timeLevel']-1, selectionObj['id'], gObj['id']);
            if(childGraphObj == null){
            	childGraphObj = new GraphObj(selectionObj['id'],gObj['id'],gObj['timeLevel']-1);
            	graphDict[childGraphObj.id] = childGraphObj;
                gObj.children.push(childGraphObj); 	// update children list of parent object
                
                updateTimeLevelDictionary(childGraphObj.timeLevel, childGraphObj);
            }
            sortSelectionListByStartX(gObj.selectionList);
            adjustCanvasPositions(gObj);
            
            // set vertex list
            /*var response = getSelectionVertices(gObj, selectionObj.canvasCoordinates[0], selectionObj.width, selectionObj.canvasCoordinates[1], selectionObj.height);
            var childVertexList = response["vertexList"];
            childGraphObj.vertexList.length = 0;	// clear
            for(var i=0; i<childVertexList.length; i++){
				var vertex = translatePipeSeparatedStringToVertObj(childVertexList[i]);
				childGraphObj.vertexList.push(vertex);	
			}*/
            var selectedVertexList = translateObjListtoIDList(gObj['vertexList']);
            updateSelectedVertices(childGraphObj, selectedVertexList, intraAction)
            
            
            childGraphObj.segmentsList = getGraphSegments(0, segWidth, selectionObj.canvasCoordinates[1], selectionObj.height, childGraphObj['width']);
            childGraphObj.refresh = 1;
            refreshTheScene();
          	selectionObj.isVisible = true;
          	updateSelectionTimeLabel(selectionObj, gObj);
            
        } else{
        	if(currentSelection !== 0){
        		
	        	mouse.startX = mouse.x;
	            mouse.startY = mouse.y;
	            
	            // remove if already exist
	            if(selectionIndex != -1){
	            	gObj['selectionList'].splice(selectionIndex, 1);
	            }
	            
            
	            // create new one
        		selectionObj = new selectionRect(currentSelection);
        		selectionObj.type = $('#selectionType option:selected').val();
        		gObj['selectionList'].push(selectionObj);
            	
            	selectionObj.canvasCoordinates[0] =  Math.floor(mouse.x - canvas.getBoundingClientRect().left); 	// relative position
            	if(selectionObj.type == '1'){
            		selectionObj.canvasCoordinates[1] =  0;
            		selectionObj.height =  canvas.height;
            	}else if(selectionObj.type == '2'){
            		selectionObj.canvasCoordinates[1] =  Math.floor(mouse.y - (canvas.getBoundingClientRect().top + window.scrollY));
            		console.log(selectionObj.canvasCoordinates[1]);
            	}
            	
	            
	            canvas.style.cursor = "crosshair";
	            
        	}
        
        }
    }
	
}

function drawSelectionRect(gObj, selectionObj, mouse){
	var selectRectId = 'selectRect_'+gObj.id+'_'+selectionObj.id;
	var selectRect = document.getElementById(selectRectId);
	if(selectRect == null){
		selectRect = createHTMLElement(selectRectId, 'div', '', '', '', '', 'selection'+selectionObj.id, '', '');
		document.getElementById('selections').appendChild(selectRect);
		
	}
	
	selectRect.style.width = selectionObj.width + 'px';
	selectRect.style.height = selectionObj.height + 'px';
	selectRect.style.left = gObj.htmlCoordinates[0]+selectionObj.canvasCoordinates[0] + 'px';
	selectRect.style.top = gObj.htmlCoordinates[1]+selectionObj.canvasCoordinates[1] + 'px';
	selectRect.onmousemove = function (e) {
    	onMouseMove(e, mouse, selectionObj, gObj);
    }
	
	updateSelectionTimeLabel(selectionObj, gObj, selectRect);
}

function onMouseMove(e, mouse, selectionObj, gObj){
	setMousePosition(e);
	updateDateTimeCursor(gObj, mouse);
    if (selectionObj !== null && !selectionObj.isVisible) {
    	selectionObj.width = Math.abs(Math.floor(mouse.x - mouse.startX));
    	if(selectionObj.type == '2')	// space & time selection
    		selectionObj.height = Math.abs(Math.floor(mouse.y - mouse.startY));
    	selectionObj.canvasCoordinates[0] = Math.floor( ((mouse.x - mouse.startX < 0) ? mouse.x : mouse.startX) - gObj.htmlCoordinates[0] );
    	drawSelectionRect(gObj, selectionObj, mouse);
    }
}

function updateDateTimeCursor(gObj, mouse){
	var cursorDiv = document.getElementById('cursor');
	var toplabel = null;
    if(cursorDiv == null){
    	cursorDiv = createHTMLElement('cursor', 'div', 10, gObj.height, mouse.x, gObj.htmlCoordinates[1], '', 'absolute', '');        
    	toplabel = createHTMLElement('topLabel_'+cursorDiv.id, 'span', '', '', '', '', 'tooltiptexttop', '', '');
    	toplabel.style.marginBottom = '20px';
        cursorDiv.appendChild(toplabel); 
        document.getElementById('cursors').appendChild(cursorDiv);
        
    }else{
    	toplabel = document.getElementById('topLabel_'+cursorDiv.id);
    }
    
	cursorDiv.style.left = mouse.x + 'px';
	cursorDiv.style.top = gObj.htmlCoordinates[1] + 'px';
	var start = Math.round(mouse.x - gObj.htmlCoordinates[0]);
	setDateTimeLabelText(toplabel, gObj, start);
        
}

function updateSelectionTimeLabel(selectionObj, gObj, selectRectDiv){
	// adjust datetime label
    var startLabelId = 'startLabel_'+gObj.id+'_'+selectionObj.id;
	var startlabel = document.getElementById(startLabelId);  
	if(startlabel == null){
		startlabel = createHTMLElement(startLabelId, 'span', '', '', '', '', 'tooltiptexttop', '', '');
		selectRectDiv.appendChild(startlabel);
	}
	
	if(selectionObj.isVisible){		// To-label
		var leftpadding = 0;
	    var labelWidth = selectionObj.width;
	    if(selectionObj.width < 200){
	    	labelWidth = 200;
	    	remainig = 200-selectionObj.width;
	    	leftpadding = remainig/2;
	    }
	    startlabel.style.width = labelWidth + 'px';
	    startlabel.style.marginLeft = '0px';
	    startlabel.style.left = -leftpadding + 'px';
	}
    
    
    var start = selectionObj.isVisible ? selectionObj.canvasCoordinates[0] + selectionObj.width : selectionObj.canvasCoordinates[0];
	setDateTimeLabelText(startlabel, gObj, start, selectionObj.isVisible);
}



/**************************** vertices list listener **************************/
function registerRefreshBtnDiv(gObj, refreshBtnDiv, zommedVerticesDiv){
	// register click event for refresh button
	$(refreshBtnDiv).find('a[class=interLink]').bind('click', function(event) {
		
		event.preventDefault();
		updateVertexList(interAction, gObj, zommedVerticesDiv);
		gObj.refresh = 1;
		hideZommedDifferanceVertexList(gObj);
		refreshTheScene();
		
	});
	
	$(refreshBtnDiv).find('a[class=intraLink]').bind('click', function(event) {
		
		event.preventDefault();
		updateVertexList(intraAction, gObj, zommedVerticesDiv);
		gObj.refresh = 1;
		hideZommedDifferanceVertexList(gObj);
		refreshTheScene();
		
	});
	
	
	// register click event for hide button
	$(refreshBtnDiv).find('a[class=hideLink]').bind('click', function(event) {
		event.preventDefault();
		hideZommedDifferanceVertexList(gObj);
	});
	
	// handle select all checkbox
	$(refreshBtnDiv).find('input[type=checkbox]').change(function() {
	    var checked = this.checked;
		$(zommedVerticesDiv).find('p').each(function() {
			$(this).find('input[type=checkbox]').prop('checked', checked);
		});
		
	});
} 

function registerMouseHover(gObj, verticesDiv, zommedVerticesDiv, menuDiv, arrow, edgeCountDiv) {
	
	var currentIndex = -1;
	var count = $(verticesDiv).find('div').length;
	var heightEachElement = 20;
	//var noElementsToShow = Math.ceil( $(verticesDiv).height() / widthEachElement);//$(zommedVerticesDiv).find('p').first().height() ;
	
	$(verticesDiv).find('div').each(function() {
		
		jQuery(this).mouseover(function(e) {
			var index = jQuery(this).index();
			showZoomedList(index, menuDiv, verticesDiv, zommedVerticesDiv, heightEachElement, arrow, edgeCountDiv);
			currentIndex = index;
			gObj.selectedVertex = $(zommedVerticesDiv).children().eq(currentIndex).find('input[type=checkbox]').val();
		});
	    
	});
	
	$(zommedVerticesDiv).bind('keydown', function(event) {
	    switch(event.keyCode){
	    	case 38: // up
	    		event.preventDefault();
	    		if(currentIndex>0){
	    			currentIndex--;
	    			showZoomedList(currentIndex, menuDiv, verticesDiv, zommedVerticesDiv, heightEachElement, arrow, edgeCountDiv)
	    			gObj.selectedVertex = $(zommedVerticesDiv).children().eq(currentIndex).find('input[type=checkbox]').val();
	    		}
	    		break;
	    	case 40: // down
	    		event.preventDefault();
	    		if(currentIndex<count-1){
	    			currentIndex++;
	    			showZoomedList(currentIndex, menuDiv, verticesDiv, zommedVerticesDiv, heightEachElement, arrow, edgeCountDiv)
	    			gObj.selectedVertex = $(zommedVerticesDiv).children().eq(currentIndex).find('input[type=checkbox]').val();
	    		}
	    		break;

            default: return; // exit this handler for other keys
	    }
	 });
	
	// show menu if there is already selected vertex
	if(gObj.selectedVertex != -1){
		var s_index = 0;
		$(zommedVerticesDiv).children().find('input[type=checkbox]').each(function() {
			if($(this).val() === gObj.selectedVertex)
				s_index = $(zommedVerticesDiv).children().index($(this).parent());
		});
		if(s_index != -1){
			currentIndex = s_index;
			showZoomedList(currentIndex, menuDiv, verticesDiv, zommedVerticesDiv, heightEachElement, arrow, edgeCountDiv)
		}
	}
	
}

function updateVertexList(actionId, gObj, zommedVerticesDiv){
	var selectedVertexList = [];
	$(zommedVerticesDiv).find('p').each(function() {
		$(this).find('input[type=checkbox]:checked').each(function() {
			selectedVertexList.push($(this).attr('value'));
		});
	});
	
	updateSelectedVertices(gObj, selectedVertexList, actionId);
	/*
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
	*/
    
}

function showZoomedList(index, menuDiv, verticesDiv, zommedVerticesDiv, heightEachElement, arrow, edgeCountDiv){
	var originalDivElements = $(verticesDiv).find('div');
	var ZoomedDiv = $(zommedVerticesDiv);
	
	var count = originalDivElements.length;
	
	// clear at first
	ZoomedDiv.children().hide(); 
	ZoomedDiv.children().css('border', 'none'); 
	$(verticesDiv).children().find('p').css('border', 'none');
	
	// load current focused element
	$(arrow).css('top', originalDivElements.eq(index).offset().top+'px');
	$(arrow).css('display', 'inline-block');
	$(verticesDiv).children().eq(index).find('p').css( "border",'red 1px solid' );
	verticesDiv.style.cursor = "pointer";
	menuDiv.style.display = 'inline-block';
	ZoomedDiv.children().eq(index).css( "display",'block' );
	ZoomedDiv.children().eq(index).css( "font-size",'15px' );
	ZoomedDiv.children().eq(index).css( "border",'black 1px solid' );
	
	$(edgeCountDiv).css('display', 'inline-block');
	var inEdgeCount = $(verticesDiv).children().eq(index).find('p[class=vertexInEdge]').attr('title');
	var outEdgeCount = $(verticesDiv).children().eq(index).find('p[class=vertexOutEdge]').attr('title');
	var mappedInEdgeCount = 0; mappedOutEdgeCount = 0;
	if(inEdgeCount != null && outEdgeCount != null){
		mappedInEdgeCount = nFormatter(inEdgeCount,1);
		mappedOutEdgeCount = nFormatter(outEdgeCount,1);
	}
	$(edgeCountDiv).find('p[class=inEdgeCount]').html(mappedInEdgeCount);
	$(edgeCountDiv).find('p[class=outEdgeCount]').html(mappedOutEdgeCount);
	
	// load before vertices in the zoomed list
	var currentPosition = index * pixelVerticalGap;
	var heightRegionBefore = currentPosition - (heightEachElement/2) - 0;
	if(heightRegionBefore > heightEachElement){
		var elementsBefore = Math.round(heightRegionBefore/heightEachElement);
		var i_prev = index-1;
		for(var i=0;i<elementsBefore;i++){
			if(i_prev>0){
				ZoomedDiv.children().eq(i_prev).css( "display",'block' );
				ZoomedDiv.children().eq(i_prev).css( "font-size",'12px' );
				
				i_prev--;
			}
			
		}
	}
	
	// load after vertices in the zoomed list
	var heightRegionAfter = $(verticesDiv).height() - (currentPosition + (heightEachElement/2));
	if(heightRegionAfter > heightEachElement){
		var elementsAfter = Math.round(heightRegionAfter/heightEachElement);
		var i_next = index+1;
		for(var i=0;i<elementsAfter;i++){
			if(i_next<count-1){
				ZoomedDiv.children().eq(i_next).css( "display",'block' );
				ZoomedDiv.children().eq(i_next).css( "font-size",'12px' );
				
				i_next++;
			}
			
		}
	}
	
	ZoomedDiv.focus();
	
}
