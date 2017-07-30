/**
 * This file includes all functions that call server-side code thru ajax request
 */

function getImageSegment(gObj, startXHeirarchy, segWidth, parentSelectionWidth, startY, segHeight){
	var startX = startXHeirarchy[0];
	var selectedVertices = translateObjListtoIDList(gObj['vertexList']);
	$.ajax({
		type: 'post',
		url: 'getImageSegment',
		data: {actionName:'getImageSegment', startXHeirarchy:startXHeirarchy, startY:startY, parentSelectionWidth:parentSelectionWidth, 
			segWidth:segWidth, segHeight:segHeight, timeLevel:gObj['timeLevel'],minEdgeWeight:gObj['minEdgeWeight'],selectedVertices:selectedVertices
		},
		success: function (output) {
			var imgData = output["imgData"];
			//var imgHeight = output["imgHeight"];
			
			/*
			if(gObj.height!==imgHeight)	// set height
				adjustCanvasHeight(gObj, imgHeight);
			*/
			
			if(typeof imgData == 'undefined'){
				clearSegment(gObj, startX, segWidth);
			}else if(imgData != ''){
				drawSegment(gObj, startX, segWidth, imgData);
			}
			
		}
	});	
	
}

function getSelectionDate(label, startXHeirarchy, timeLevel, isAppend){
	$.ajax({
		type: 'post',
		url: 'getImageSegment',
		data: {actionName:'getSelectionDate',startXHeirarchy:startXHeirarchy,timeLevel:timeLevel},
		success: function (output) {
			if(isAppend){
				document.getElementById(label).innerHTML += ' - '+output["dateTimeString"];
			}else{
				document.getElementById(label).innerHTML = output["dateTimeString"];	
			}
			
		}
	});
	
}

/*
function getSelectionVertices(gObj, startX, selectionWidth, startY, selectionHeight){
	var startXHeirarchy = getSelectionHierarchy(startX, gObj);
	var selectedVertices = translateObjListtoIDList(gObj['vertexList']);
	var theResponse = null;
	$.ajax({
		type: 'post',
		url: 'getImageSegment',
		data: {actionName:'getRegionVertices', startXHeirarchy:startXHeirarchy, selectionWidth:selectionWidth, startY:startY, 
			selectionHeight:selectionHeight, timeLevel:gObj['timeLevel'], minEdgeWeight:gObj['minEdgeWeight'], selectedVertices:selectedVertices
		},
		async: false,
		success: function (output) {
			theResponse = output;
		}
		
	});
	
	return theResponse;
}
*/

function filterSelectedVertices(gObj, startX, parentSelectionWidth, startY, parentSelectionHeight, selectedVertices, relationType){
	var startXHeirarchy = getSelectionHierarchy(startX, gObj);
	var theResponse = null;
	$.ajax({
		type: 'post',
		url: 'getImageSegment',
		data: {actionName:'filterSelectedVertices', startXHeirarchy:startXHeirarchy, parentSelectionWidth:parentSelectionWidth, startY:startY, 
			parentSelectionHeight:parentSelectionHeight, timeLevel:gObj['timeLevel'], minEdgeWeight:gObj['minEdgeWeight'], selectedVertices:selectedVertices,
			relationType:relationType
		},
		async: false,
		success: function (output) {
			theResponse = output;
		}
		
	});
	
	return theResponse;
}


