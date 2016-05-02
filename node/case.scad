length = 47;
width = 31;
depth = 10;
thickness = 3;

usb_width = 5;
usb_height = 3;
usb_x = 15;
usb_y = 5;

hole_width = 5;
hole_length = 10;
hole_x = 10;
hole_y = (width+2*thickness-hole_length)/2;

difference () {
	cube([length + (thickness * 2), width + (thickness * 2), depth + thickness]);
	translate([thickness, thickness, thickness])
		cube([length, width, depth + 1]);
	translate([-.5, usb_x, usb_y])
		cube([thickness + 1, usb_width, usb_height]);
}

translate([0,0, depth*2]) {
	difference() {
		union() {
			cube([length + (thickness *2), width + (thickness *2), thickness]);
			translate([thickness ,thickness, -thickness/2])
				cube([length, width, thickness/2]);
		}

		translate([hole_x, hole_y, -thickness*1])
			cube([hole_width, hole_length, thickness*3]);
	}
}
