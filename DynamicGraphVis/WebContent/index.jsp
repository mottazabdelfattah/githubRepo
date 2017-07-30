<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
<script src="JS/jquery-3.1.1.min.js"></script>
<link rel="stylesheet" type="text/css" href="style.css">
</head>
<body>
Hello World


<div id="vertexlist1" class="vertexlist">
	<p>test1</p>
	<p>test2</p>
	<p>test3</p>
	<p>test4</p>
	<p>test5</p>
	<p>test6</p>
	<p>test7</p>
	<p>test8</p>
	<p>test9</p>
</div>
<div id="zoomed_vertexlist1"class="vertexlist-zoom">
	<p>test1</p>
	<p>test2</p>
	<p>test3</p>
	<p>test4</p>
	<p>test5</p>
	<p>test6</p>
	<p>test7</p>
	<p>test8</p>
	<p>test9</p>
</div>
<script type="text/javascript">
$('#vertexlist1 p').each(function() {
	var count = $("#vertexlist1 p").length;
	jQuery(this).mouseover(function() {
		$("#zoomed_vertexlist1").children().hide(); 
		var index = (jQuery(this).index());
		$('#zoomed_vertexlist1').children().eq(index).css( "display",'block' );
		$('#zoomed_vertexlist1').children().eq(index).css( "font-size",'30px' );
		
		
		if(index>0){
			$('#zoomed_vertexlist1').children().eq(index-1).css( "display",'block' );
			$('#zoomed_vertexlist1').children().eq(index-1).css( "font-size",'25px' );
			
		}
		if(index<count-1){
			$('#zoomed_vertexlist1').children().eq(index+1).css( "display",'block' );
			$('#zoomed_vertexlist1').children().eq(index+1).css( "font-size",'25px' );
		}
	});
    
});
</script>
</body>
</html>
