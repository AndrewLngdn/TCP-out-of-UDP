README.txt for assignment 2

Andrew Langdon
arl2178

The code in this folder, when compiled, matches the specifications
given by the assignment. It is written in Java and can be compiled
by using the 'make' command. 

Compile:

	$ make

Start the Receiver first:

	$ java Receiver foo.txt 5000 localhost 6000 stdout

The above line receives whatever file the sender sends and 
names it foo.txt

Then run the sender:

	$ java Sender Sender.java localhost 5000 6000 5 stdout

The above line sends the Sender source as the transfer file.


+---------<<
TCP is recreated using UDP in a style most similar to GBN. 

Segment size is 576 bytes

The code sends a fin with the last packet.

Sequences start from 0.

The code supports arbitrary window sizes above 0. 

All packets are logged with packet info when the info is meaningful,
and uses 'n/a' when it isn't.

The retransmission timer is doubled when a timeout occurs, and reduces 
by 50ms when a packet, being careful that he timeout doesn't get to 0.
// change this to TCP style 

The TCP header is 20 bytes, with the unused bytes padded with undefined
numbers. Only the fin flag is relevant and the header size is always 20
bytes, so that entire region is represented by a 1 if the fin flag is set
or a 0 if it is not.

// update the checksum so it goes over the header but not the 
// checksum part. 

