package main

import (
	"fmt"

	"path/filepath"
)

type userSetting struct {
	Scope string
}

//main
func main() {
	// fuzzysearchMatch("JCDXX02A*.AFP")

	// bill := animals.Getbill("meow")
	// fmt.Println(reflect.TypeOf(bill))
	// fmt.Printf("%+v\n", bill.Animal)
}

//模糊搜尋練習
func fuzzysearchMatch(s string) {

	matches := filepath.Base("/jjj/aaa/a.txt")
	fmt.Print(matches)
}

/*
	target := []string{"BKXXMTTW.B.PA08.100318.103325.0002.XXXXXX.AFP",
		"BKXXMTTW.C.PA08.100318.103325.0002.XXXXXX.AFP",
		"BKXXMTTW.D.PA08.100318.103325.0002.XXXXXX.AFP",
		"BKXXMTTW.E.PA08.100318.103325.0002.XXXXXX.AFP",
		"CIGAXX99.A.PB90.20190516.161216.MESG.000001.AFP",
		"CIGAXX99.A.PB90.20190529.130104.MESG.000001.AFP",
		"CIGAXX99.A.PB90.20190617.142108.MESG.000001.AFP",
		"CIGAXX99.A.PB90.20190628.172257.MESG.000001.AFP",
		"CIGAXX99.E.PA02.20190516.161216.MESG.XXXXXX.AFP",
		"CIGAXX99.E.PA02.20190529.130104.MESG.XXXXXX.AFP",
		"CIGAXX99.E.PA02.20190617.142108.MESG.XXXXXX.AFP",
		"CIGAXX99.E.PA02.20190628.172257.MESG.XXXXXX.AFP",
		"CIGMXX99.A.PA08.20190516.161206.MESG.000001.AFP",
		"CIGMXX99.A.PA08.20190529.130044.MESG.000001.AFP",
		"CIGMXX99.A.PA08.20190617.142058.MESG.000001.AFP",
		"CIGMXX99.A.PA08.20190628.172247.MESG.000001.AFP",
		"CIGMXX99.E.PA02.20190516.161206.MESG.XXXXXX.AFP",
		"CIGMXX99.E.PA02.20190529.130044.MESG.XXXXXX.AFP",
		"CIGMXX99.E.PA02.20190617.142058.MESG.XXXXXX.AFP",
		"CIGMXX99.E.PA02.20190628.172247.MESG.XXXXXX.AFP",
		"CTCPNGXR.D.TEST.20190614.131106.PC19.XXXXXX.AFP",
		"EXD02001.A.PAC1.190510.172322.0001.336806.336806.AFP",
		"EXD02001.A.PAC1.190510.172424.0001.336806.336806.AFP",
		"EXD02001.A.PAC1.190510.173124.0001.336814.336814.AFP",
		"EXD10601.B.CH07.190510.183824.0001.663553.AFP",
		"EXD10601.B.CH07.190618.143854.0001.999999.AFP",
		"IMSMD315.A.PA08.190510.165052.0001.195948.AFP",
		"JCDXX02A.A.PA01.190629.091550.TEST.000001.AFP",
		"JCDXX02A.B.PA01.190629.091550.TEST.000001.AFP",
		"JCDXX02A.C.PA01.190629.091550.TEST.000001.AFP",
		"JCDXX08Q.A.PC96.190629.091529.TEST.000001.AFP",
		"JCDXX08Q.C.PA08.190629.091529.TEST.000001.AFP",
		"JCDZD001.D.CH07.190327.133739.T500.001404.AFP",
		"JCDZD001.D.CH07.190327.134032.T500.001404.AFP",
		"JCDZD001.D.CH07.190327.134236.T500.001404.AFP",
		"JCDZD001.D.CH07.190327.134540.T500.001404.AFP",
		"JCDZD001.D.CH07.190327.134945.T500.001404.AFP",
		"JCDZD001.D.CH07.190627.150214.T500.001447.AFP",
		"JCDZD001.D.CH07.190627.150316.T500.001454.AFP",
		"JCDZD001.D.CH07.190627.150326.T500.001452.AFP",
		"JCDZD001.D.CH07.190627.150337.T500.001448.AFP",
		"JCDZD001.D.CH07.190627.150408.T500.001457.AFP",
		"JCDZD001.D.CH07.190627.150418.T500.001456.AFP",
		"JCDZD001.D.CH07.190627.150429.T500.001455.AFP",
		"JCDZD001.D.CH07.190627.150510.T500.001470.AFP",
		"JCDZD001.D.CH07.190627.150521.T500.001458.AFP",
		"JCDZD001.D.CH07.190627.150613.T500.001484.AFP",
		"JCDZD001.D.CH07.190627.150623.T500.001483.AFP",
		"JCDZD001.D.CH07.190627.150634.T500.001481.AFP",
		"JCDZD001.D.CH07.190627.150715.T500.001487.AFP",
		"JCDZD001.D.CH07.190627.150726.T500.001486.AFP",
		"JCDZD001.D.CV05.190327.133739.T500.001404.AFP",
		"JCDZD001.D.CV05.190327.134032.T500.001404.AFP",
		"JCDZD001.D.CV05.190327.134236.T500.001404.AFP",
		"JCDZD001.D.CV05.190327.134540.T500.001404.AFP",
		"JCDZD001.D.CV05.190327.134945.T500.001404.AFP",
		"JCDZD001.D.CV05.190627.150214.T500.001447.AFP",
		"JCDZD001.D.CV05.190627.150316.T500.001454.AFP",
		"JCDZD001.D.CV05.190627.150326.T500.001452.AFP",
		"JCDZD001.D.CV05.190627.150337.T500.001448.AFP",
		"JCDZD001.D.CV05.190627.150408.T500.001457.AFP",
		"JCDZD001.D.CV05.190627.150418.T500.001456.AFP",
		"JCDZD001.D.CV05.190627.150429.T500.001455.AFP",
		"JCDZD001.D.CV05.190627.150510.T500.001470.AFP",
		"JCDZD001.D.CV05.190627.150521.T500.001458.AFP",
		"JCDZD001.D.CV05.190627.150613.T500.001484.AFP",
		"JCDZD001.D.CV05.190627.150623.T500.001483.AFP",
		"JCDZD001.D.CV05.190627.150634.T500.001481.AFP",
		"JCDZD001.D.CV05.190627.150715.T500.001487.AFP",
		"JCDZD001.D.CV05.190627.150726.T500.001486.AFP",
		"JCDZD001.D.PA08.190327.133739.T500.001404.AFP",
		"JCDZD001.D.PA08.190327.134032.T500.001404.AFP",
		"JCDZD001.D.PA08.190327.134236.T500.001404.AFP",
		"JCDZD001.D.PA08.190327.134540.T500.001404.AFP",
		"JCDZD001.D.PA08.190327.134945.T500.001404.AFP",
		"JCDZD001.D.PA08.190627.150214.T500.001447.AFP",
		"JCDZD001.D.PA08.190627.150316.T500.001454.AFP",
		"JCDZD001.D.PA08.190627.150326.T500.001452.AFP",
		"JCDZD001.D.PA08.190627.150337.T500.001448.AFP",
		"JCDZD001.D.PA08.190627.150408.T500.001457.AFP",
		"JCDZD001.D.PA08.190627.150418.T500.001456.AFP",
		"JCDZD001.D.PA08.190627.150429.T500.001455.AFP",
		"JCDZD001.D.PA08.190627.150510.T500.001470.AFP",
		"JCDZD001.D.PA08.190627.150521.T500.001458.AFP",
		"JCDZD001.D.PA08.190627.150613.T500.001484.AFP",
		"JCDZD001.D.PA08.190627.150623.T500.001483.AFP",
		"JCDZD001.D.PA08.190627.150634.T500.001481.AFP",
		"JCDZD001.D.PA08.190627.150715.T500.001487.AFP",
		"JCDZD001.D.PA08.190627.150726.T500.001486.AFP",
		"JCXX08GP.A.PA01.190702.133727.TEST.000001.AFP",
		"JCXXX08A.A.PA01.190702.162342.TEST.000001.AFP",
		"JCXXX08A.B.PA01.190702.162342.TEST.000001.AFP",
		"JCXXX08A.C.PA01.190702.162342.TEST.000001.AFP",
		"NA070201.S.PA08.190624.161657.0001.000024.AFP",
		"NA49TEST.D.NA49.190620.140352.0001.000001.AFP",
		"S1PD52E1.S.PA08.190626.152314.0001.XXXXXX.AFP",
		"SHM00713.A.SAMP.190514.095130.0001.337049.337049.AFP",
		"TRUST212.S.CH07.190502.112810.TEST.MMESGX.AFP",
		"TRUST212.S.SAMP.190502.112810.TEST.MMESGX.AFP",
		"TRUST213.S.SAMP.190502.112810.TEST.MMESGX.AFP",
	}
*/
