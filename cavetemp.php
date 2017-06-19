<?php
$db_name = "sharedpot";
$mysql_username = "";
$mysql_password = "";
$server_name= "";
$con=mysqli_connect($server_name,$mysql_username,$mysql_password,$db_name);

if (mysqli_connect_errno($con))
{
   echo "Failed to connect to MySQL: " . mysqli_connect_error();
}
$date = $_POST['date'];
$result = mysqli_query($con,"SELECT temp,date FROM cavetemps WHERE
date LIKE '$date'");
while($row = mysqli_fetch_array($result))
{
	echo $row[0];
	echo "_";
	echo $row[1];
	echo "/";
}
mysqli_close($con);
?>