# Changelog for public releases of Maat

## Version 0.1-VeReMi

This version contains the work done for the VeReMi paper, currently under review for SecureComm 2018.

Most significant changes are listed below.

### Detectors

 - fix an initialization error in the factory of most detectors:
   - `null` is now returned if a non-existent field is set through `detectors.xml`.
   - This fail-fast behavior simplifies debugging.
 - Parameters for several detectors were renamed to be shorter (since they're part of every line of detection output).
 - Default values are now set for a variety of detectors, which are not part of the output, and not by default set through `detectors.xml` -- it is still possible to overwrite these values through `detectors.xml`, though.
 - Renamed `DistanceMovedVerifier` to `eDMV`
 - Corrected `eDMV` behavior: previously, output of complete uncertainty led to all-positive evaluation.
 - `eSAW` was corrected to assign belief for a value over the threshold, instead of simply selecting uncertainty as 1, which caused all-positive evaluation.
 - `eOPD` has a new parameter, `RECTANGLE_COUNT`, replacing the constant value in the previous version.
 - `SSC` was renamed to `eSSC`, and the corresponding parameters were also renamed.
 - `eSSC` now outputs a non-dogmatic opinion when it disagrees.

### Maat Core

 - a bug was fixed that caused Maat not to terminate when a parser threw an exception. Maat now exits with a error code 1.
 - subjective logic libraries were updated to the most recent version.

### Configuration &amp; Documentation

**Deprecations**

 - job scripts should be considered deprecated from this version onwards.

Changes:

 - a number of todos were removed from `detectors.xml`, and the list now includes the parameters used for the VeReMi paper.
 - modifications were mode to the job running component, including increased run time and the location of the JSON input.
 - some warnings have been changed to become trace information instead (i.e., no more superfluous output).
 - Logging is now done to log files (in `.`), by default only warnings and above.
 - a new set of test data was added for validation (i.e., checking that Maat was correctly configured and deployed) (`testdata/JSONlog-0-short.json`).

### Data Analysis

This is moved to a separate repository, since it is done on a per-paper basis, and the code could be ported for other projects.


----


## Version 0.1b2 (2018-02-01)

Fixed contributors file (before publication)

## Version 0.1b1 (2018-01-29)

This was intended to be the public beta release published to Github, and should not be considered stable.
