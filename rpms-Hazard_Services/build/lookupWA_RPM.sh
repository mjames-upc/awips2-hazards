#!/bin/bash

function lookupWA_RPM()
{
	# Arguments
	#	1) name of the rpm to lookup
	#	2) WA RPM build directory root
	
	# lookup the rpm
	if [ "${1}" = "awips2-edex-hazards" ]; then
		export RPM_SPECIFICATION="${2}/Installer.edex-hazards"
		return 0	
	fi
	
	return 1
}
