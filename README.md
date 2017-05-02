# Handling "blindspots" in mobile internet connectivity - A multipath approach
IU Bloomington Class Project of - Mobile Computing

## Running the program:
The project uses gradle for build automation and downloading dependencies. If you're facing issues related to gradle, please refer to: https://gradle.org/docs 
**Note:** It is recommended to run this program on a windows machine. Even though the program runs fine in other machines, the apache common package used in this project has some platform dependent code and hence, the program behavior may vary when run on different platforms. The results mentioned in the paper can be reproduced when run on Windows. 
>>Tested on: Java 1.8
**STEPS:**
* Clone this project
* Open windows command prompt or terminal and change the directory to the base directory of this project (MPTCPHandover)
* Provide execute permissions to the files (chmod for linux)
* run: `gradlew build`
* The above command will download gradle,its dependencies and builds the code.
* [Skip this step to apply default values] Go to ./MPTCPHandover/MPTCPCore/src/main/resources/config.properties and set suitable values for the variables:
  * host = sets the host where the server will be running
  * port = the port number to listen to
  * server_directory_name = the directory where the files are stored
  * client_directory_name = the download directory
  * server_directory = The path of server_directory_name [default: working directory]
  * client_directory = The path of client_directory_name [default: working directory]
  * file = the file to download.
  If no changes are made, the server will listen to localhost on port 23875 and directories are referenced from current working directory.
* To run the server, run command: `gradlew run`
* Open a new terminal/command prompt, navigate to the current directory and run: `gradlew client`

