#!/bin/bash

##
# This software was developed and / or modified by Raytheon Company,
# pursuant to Contract DG133W-05-CQ-1067 with the US Government.
# 
# U.S. EXPORT CONTROLLED TECHNICAL DATA
# This software product contains export-restricted data whose
# export/transfer/disclosure is restricted by U.S. law. Dissemination
# to non-U.S. persons whether in the United States or abroad requires
# an export license or other authorization.
# 
# Contractor Name:        Raytheon Company
# Contractor Address:     6825 Pine Street, Suite 340
#                         Mail Stop B8
#                         Omaha, NE 68106
#                         402.291.0100
# 
# See the AWIPS II Master Rights File ("Master Rights File.pdf") for
# further licensing information.
#

##############################################################################
# Setup utilities for Hazard Services Backup.
#
# SOFTWARE HISTORY
# Date         Ticket#    Engineer    Description
# ------------ ---------- ----------- --------------------------
# Sep 14, 2015 3473       Chris.Cody  Initial version
# Nov 23, 2015 3473       Robert.Blum Changes to properties files.
# Nov 03, 2016 22119      Kevin.Bisanz Improve error handling, add PID to lock file.
##
if [ ${#AWIPS_HOME} = 0 ]
then
    AWIPS_HOME=/awips2
fi

source ${AWIPS_HOME}/edex/data/utility/edex_static/base/HazardServices/ServiceBackup/configuration/svcbu.properties

function configureLogging()
{
    local program_name=${1}
    local siteid=${2}
    if [[ -n "${siteid}" ]]
    then
        siteid=`echo ${siteid} | tr [a-z] [A-Z]`
    fi
    
    if [[ -n "${siteid}" ]]
    then
        local log_dir=${HAZARD_SERVICES_LOG}/${siteid}/`date +%Y%m%d`
    else
        local log_dir=${HAZARD_SERVICES_LOG}/`date +%Y%m%d`
    fi
    local log_basename=${program_name}_`date +%H%M%S`
    local log_file=${log_dir}/${log_basename}
    
    ensurePathExists "${log_dir}"
    touch ${log_file}
    exec 1>${log_file} 2>&1
}

function ensurePathExists()
{
    if [[ -n "$1" ]]
    then
        local path="$1"
        [ ! -d ${path} ] && (umask 000;mkdir -p ${path})

        # Ensure the directory was actually created.
        if [ ! -d ${path} ]
        then
            echo "Error creating directory: $path" >&2
            return 1
        fi
    fi
}

function getLockFile()
{
    local lockname="$1"
    local siteid=`echo ${2} | tr [a-z] [A-Z]`
    
    local site_lock_path="${HS_LOCK_DIR}/${siteid}"
    ensurePathExists "${site_lock_path}"
    
    echo "${site_lock_path}/${lockname}"
}

function isOperationInProgress()
{
    local lockname="$1"
    local siteid=`echo ${2} | tr [a-z] [A-Z]`
    
    local site_lock_path=$(getLockFile ${lockname} ${siteid})
    
    local retval=false
    if [[ -f ${site_lock_path} ]]
    then
        local status=`cat ${site_lock_path} | head -n 1 | awk '{print $1}'`
        if [[ "${status}" = "IN_PROGRESS" ]]
        then
            retval=true
        fi
    fi        
    
    echo "${retval}"
}

function isOperationInProgressByMe()
{
    local lockname="$1"
    local siteid=`echo ${2} | tr [a-z] [A-Z]`

    local site_lock_path=$(getLockFile ${lockname} ${siteid})

    local retval=false
    if [[ -f ${site_lock_path} ]]
    then
        local status=`cat ${site_lock_path} | head -n 1 | awk '{print $1}'`
        local pid=`cat ${site_lock_path} | head -n 1 | awk '{print $3}'`
        if [[ "${status}" = "IN_PROGRESS" ]] && [[ "$pid" = $$ ]]
        then
            retval=true
        fi
    fi

    echo "${retval}"
}

function getTempDirectory()
{
    local program_name=`echo ${1} | tr [A-Z] [a-z]`
    local siteid=`echo ${2} | tr [a-z] [A-Z]`
    
    local prog_temp_path="${HS_SVCBU_HOME}/${siteid}/${program_name}"
    ensurePathExists "${prog_temp_path}"
    
    echo "${prog_temp_path}"
}

function getSiteIdFromTarFile()
{
    local tar_file="$1"
    
    local siteid=""
    if [[ -f ${tar_file} ]]
    then
        siteid=`tar -xOf ${tar_file} siteID.txt | tr '[A-Z]' '[a-z]'` 
    fi
    
    echo "${siteid}"
}

function markTask()
{
    local lock_file="$1"
    local task_status="$2"
    echo ${task_status} > ${lock_file}
}

function markTaskSuccess()
{
    local lock_file="$1"
    markTask ${lock_file} "SUCCESS"
}

function markTaskFailed()
{
    local lock_file="$1"
    markTask ${lock_file} "FAILED"
}

function markTaskInProgress()
{
    local lock_file="$1"
    markTask ${lock_file} "IN_PROGRESS by $$"
}
