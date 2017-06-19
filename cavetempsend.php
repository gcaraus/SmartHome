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
$temp = $_POST['temp'];
$result = mysqli_query($con,"INSERT INTO cavetemps (temp) VALUES ($temp)");
if (mysqli_query($con, $result)) {
    echo "success";
} else {
    echo "error: " . $result . "<br>" . mysqli_error($con);
}
mysqli_close($con);
?>