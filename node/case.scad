length = 48;
width = 46;
depth = 21;
thickness = 2;

usb_width = 10;
usb_height = 5;
usb_x = 16 + thickness;
usb_y = 1 + thickness;

hole_width = 16;
hole_length = 8;
hole_x = 16 + thickness;
hole_y = (width+2*thickness-hole_length)/2;

difference () {
	cube([length + (thickness * 2), width + (thickness * 2), depth + thickness]);
	translate([thickness, thickness, thickness])
		cube([length, width, depth + 1]);
	translate([-.5, usb_x, usb_y])
		cube([thickness + 1, usb_width, usb_height]);
}

rotate([180,0,0]) {
	translate([0,10,-thickness]) {
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
}
