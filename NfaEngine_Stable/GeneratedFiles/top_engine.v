module top_engine(out,stop,clk_in,sod,en,char,eod);
	input [7:0] char;
	input clk_in,sod,en,eod;
	output stop;
 	wire [7:0] char_int;
	wire en_int;
	output [0:0] out;

	assign clk = ~clk_in;
	interfacer I1(stop,char_int,en_int,en,char,sod,eod,clk);
	BRAM_0 blockram_0 (out[0],clk,sod,en_int,char_int);

endmodule