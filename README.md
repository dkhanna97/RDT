Reliable Data Transfer on an unreliable network
By Devruth Khanna

=========



* Uses the Selective Repeat protocol to transfer a text file from one host to another across an unreliable network. 
The protocol can handle network errors such as packet loss and duplicate packets. Moreover, it guarantees in-order delivery of packets. Packets are sent from the sender program which runs on Host1 to nEmulator using UDP sockets. The packets are then forwarded from the emulator to the receiver program on Host2. Note that nEmulator is to be running on a host different from both sender and receiver hosts. Given the input probability passed from user, nEmulator will drop packets with this given probability. 

* Example Execution:
Host 1 - ubuntu1204-002: ./nEmulator-linux386 9991 ubuntu1204-006 9994 9993 ubuntu1204-004 9992 1 0.1 1
Host 2 - ubuntu1204-006: java receiver ubuntu1204-002.student.cs.uwaterloo.ca 9993 9994 output.txt
Host 3 - ubuntu1204-004: java sender ubuntu1204-002.student.cs.uwaterloo.ca 9991 9992 data.txt

* Compilers used:
GNU Make 3.81, javac 1.6.0_31

* Instructions:
To compile, type make.
To run, refer to assignment for instructions on usage, or refer to sample usage above.

Note that code may not be very clean in some places, and best practices were not used, as I had very limited time to complete the project.
