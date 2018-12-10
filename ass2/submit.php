<?xml version = "1.0"  encoding = "utf-8" ?>
<!DOCTYPE html PUBLIC "-//w3c//DTD XHTML 1.0 Strict//EN"
  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <title> Order Receipt </title>
    </head>

    <body>
    <!-- Retrieve data from client side -->
    <?php
        // Get order data values
        $name = $_POST["name"];
        $apple_count = $_POST["apple"];
        $orange_count = $_POST["orange"];
        $banana_count = $_POST["banana"];
        $payment = $_POST["payment"];

        // If any of the quantities are blank, set them to zero
        if ($apple_count == "") $apple_count = 0;
        if ($orange_count == "") $orange_count = 0;
        if ($banana_count == "") $banana_count = 0;

        // Compute sub-total and total cost
        $apple_cost = 0.69 * $apple_count;
        $orange_cost = 0.59 * $orange_count;
        $banana_cost = 0.39 * $banana_count;
        $total_cost = $apple_cost + $orange_cost + $banana_cost;  
    ?>

    <!-- Print receipt table -->
    <p><h2> Order Receipt </h2></p>

    <?php
        print ("<strong> Customer Name: </strong> $name <br />");
    ?>

    <table border="border">
        <caption><strong> Order Information </strong></caption>
        <tr>
            <th> Fruit </th>
            <th> Unit Price </th>
            <th> Quantity </th>
            <th> Cost </th>
        </tr>
        <tr align="center">
            <td> Apples </td>
            <td> 0.69 </td>
            <td> <?php print ("$apple_count"); ?> </td>
            <td> <?php printf ("$ %4.2f", $apple_cost); ?> </td>
        </tr>
        <tr align="center">
            <td> Oranges </td>
            <td> 0.59 </td>
            <td> <?php print ("$orange_count"); ?> </td>
            <td> <?php printf ("$ %4.2f", $orange_cost); ?> </td>
        </tr>
        <tr align="center">
            <td> Bananas </td>
            <td> 0.39 </td>
            <td> <?php print ("$banana_count"); ?> </td>
            <td> <?php printf ("$ %4.2f", $banana_cost); ?> </td>
        </tr>
        <tr align="center" >
            <td colspan="2"><strong> Payment method: </strong> </td>
            <td colspan="2"> <?php printf ($payment); ?> </td>
        </tr>
        <tr align="center">
            <td colspan="2"><strong> Total : </strong> </td>
            <td colspan="2"> <?php printf ("$ %4.2f", $total_cost); ?> </td>
        </tr>
    </table>

    <a href="./form.html"> Back to Order Form </a>

    <!-- Update order.txt -->
    <?php
        $apple_count = $_POST["apple"]; 
        $orange_count = $_POST["orange"];
        $banana_count = $_POST["banana"]; 
        $file = './order.txt';

        //checks if file exists
        if(file_exists($file)){
            if($fhandle = fopen($file,"r")){
                //tests if at EOF, break if at EOF
                while (!feof($fhandle)){
                    $contents[] = fgets($fhandle);
                }
                fclose($fhandle);
            }
        } else {
            fopen($file,"c+");//create file if does not exist
        }
    
        //opens the file and read line by line
        $myfile_line = file($file); 
  
        //if it is an empty text file, fills every row with empty space to prevent error
        for ($x = 0; $x <= 10; $x++) {
            if(empty($myfile_line[$x])) $myfile_line[$x]=" ";
        }

        //replaces anything which is not a number with "" line by line
        $old_apple_count = preg_replace("/[^0-9]/", "", $myfile_line[0]); 
        $old_orange_count = preg_replace("/[^0-9]/", "", $myfile_line[1]); 
        $old_banana_count = preg_replace("/[^0-9]/", "", $myfile_line[2]);  

        //sums the existing value in the text file and the newly entered value
        $new_apple_count = (int)$old_apple_count + $apple_count; 
        $new_orange_count = (int)$old_orange_count + $orange_count; 
        $new_banana_count = (int)$old_banana_count + $banana_count; 
  
        //contents to be written back to the file
        $apple_order = "Total number of apples: $new_apple_count\r\n";
        $orange_order = "Total number of oranges: $new_orange_count\r\n";
        $banana_order = "Total number of bananas: $new_banana_count\r\n";

        //writes back into the file
        $file = fopen($file,"c");
        fwrite ($file,$apple_order);
        fwrite ($file,$orange_order);
        fwrite ($file,$banana_order);
        fclose($file);
    ?>
  
    </body>
</html>