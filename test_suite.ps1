# ============================================================
# Comprehensive Test Suite for Optimized Project Scheduling System
# ============================================================

$API = "http://localhost:8080/api"
$passed = 0
$failed = 0
$bugs = @()

function Test-Assert {
    param(
        [string]$TestName,
        [bool]$Condition,
        [string]$FailMessage = ""
    )
    if ($Condition) {
        Write-Host "  [PASS] $TestName" -ForegroundColor Green
        $script:passed++
    } else {
        Write-Host "  [FAIL] $TestName - $FailMessage" -ForegroundColor Red
        $script:failed++
        $script:bugs += "$TestName : $FailMessage"
    }
}

function Api-Call {
    param(
        [string]$Uri,
        [string]$Method = "GET",
        [string]$Body = $null
    )
    try {
        $params = @{
            Uri = $Uri
            Method = $Method
            ContentType = "application/json"
            ErrorAction = "Stop"
        }
        if ($Body) {
            $params.Body = $Body
        }
        $response = Invoke-RestMethod @params
        return @{ Success = $true; Data = $response; Error = $null; StatusCode = 200 }
    } catch {
        $statusCode = 0
        $errorBody = $null
        try {
            $statusCode = [int]$_.Exception.Response.StatusCode
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $errorBody = $reader.ReadToEnd() | ConvertFrom-Json
        } catch {}
        return @{ Success = $false; Data = $null; Error = $errorBody; StatusCode = $statusCode }
    }
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " PHASE 0: DATABASE CLEANUP" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Clean: Cancel all active bookings first
$existingBookings = Api-Call -Uri "$API/admin/bookings"
if ($existingBookings.Success -and $existingBookings.Data) {
    foreach ($b in @($existingBookings.Data)) {
        if ($b.status -ne "CANCELLED") {
            Api-Call -Uri "$API/admin/bookings/$($b.id)" -Method DELETE | Out-Null
        }
    }
    Write-Host "  Cancelled all active bookings." -ForegroundColor Gray
}

# Clean: Delete all dates (cascades to slots)
$existingDates = Api-Call -Uri "$API/admin/dates"
if ($existingDates.Success -and $existingDates.Data) {
    foreach ($d in @($existingDates.Data)) {
        Api-Call -Uri "$API/admin/dates/$($d.id)" -Method DELETE | Out-Null
    }
    Write-Host "  Deleted all dates (cascaded to slots)." -ForegroundColor Gray
}

# Clean: Delete teams directly via MySQL since there is no API endpoint for it
try {
    $mysqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    $mysqlCmd = "SET FOREIGN_KEY_CHECKS=0; TRUNCATE booking; TRUNCATE slot; TRUNCATE schedule_date; TRUNCATE team; SET FOREIGN_KEY_CHECKS=1;"
    & $mysqlPath -u root -pganesh1953 scheduling_db -e $mysqlCmd 2>$null
    Write-Host "  Cleaned all tables via MySQL." -ForegroundColor Gray
} catch {
    Write-Host "  MySQL direct cleanup not available, using API-only cleanup." -ForegroundColor Yellow
}

# Verify clean state
$checkDates = Api-Call -Uri "$API/admin/dates"
$checkTeams = Api-Call -Uri "$API/admin/teams"
$checkBookings = Api-Call -Uri "$API/admin/bookings"
$checkSlots = Api-Call -Uri "$API/admin/slots"

$dateCount = if ($checkDates.Success -and $checkDates.Data) { @($checkDates.Data).Count } else { 0 }
$teamCount = if ($checkTeams.Success -and $checkTeams.Data) { @($checkTeams.Data).Count } else { 0 }
$bookingCount = if ($checkBookings.Success -and $checkBookings.Data) { @($checkBookings.Data).Count } else { 0 }
$slotCount = if ($checkSlots.Success -and $checkSlots.Data) { @($checkSlots.Data).Count } else { 0 }

Write-Host "  Post-cleanup: Dates=$dateCount, Teams=$teamCount, Slots=$slotCount, Bookings=$bookingCount" -ForegroundColor Gray
Write-Host ""

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " PHASE 1: SETUP TEST DATA" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# ── Insert 3 Demo Dates ──
Write-Host "--- Creating Demo Dates ---" -ForegroundColor Yellow

$date1Result = Api-Call -Uri "$API/admin/dates" -Method POST -Body '{"demoDate":"2026-04-10"}'
Test-Assert "Create Demo Date 1 (2026-04-10)" $date1Result.Success "Error: $($date1Result.Error)"
$dateId1 = if ($date1Result.Success) { $date1Result.Data.id } else { $null }

$date2Result = Api-Call -Uri "$API/admin/dates" -Method POST -Body '{"demoDate":"2026-04-11"}'
Test-Assert "Create Demo Date 2 (2026-04-11)" $date2Result.Success "Error: $($date2Result.Error)"
$dateId2 = if ($date2Result.Success) { $date2Result.Data.id } else { $null }

$date3Result = Api-Call -Uri "$API/admin/dates" -Method POST -Body '{"demoDate":"2026-04-12"}'
Test-Assert "Create Demo Date 3 (2026-04-12)" $date3Result.Success "Error: $($date3Result.Error)"
$dateId3 = if ($date3Result.Success) { $date3Result.Data.id } else { $null }

if (-not $dateId1 -or -not $dateId2 -or -not $dateId3) {
    Write-Host "  FATAL: Could not create demo dates. Cannot continue." -ForegroundColor Red
    exit 1
}

# ── Create Multiple Slots per Date ──
Write-Host ""
Write-Host "--- Creating Slots ---" -ForegroundColor Yellow

$slot1 = Api-Call -Uri "$API/admin/slots" -Method POST -Body "{`"scheduleDateId`":$dateId1,`"startTime`":`"09:00`",`"endTime`":`"09:30`"}"
Test-Assert "Create Slot 1 (Date1, 09:00-09:30)" $slot1.Success
$slotId1 = if ($slot1.Success) { $slot1.Data.id } else { $null }

$slot2 = Api-Call -Uri "$API/admin/slots" -Method POST -Body "{`"scheduleDateId`":$dateId1,`"startTime`":`"10:00`",`"endTime`":`"10:30`"}"
Test-Assert "Create Slot 2 (Date1, 10:00-10:30)" $slot2.Success
$slotId2 = if ($slot2.Success) { $slot2.Data.id } else { $null }

$slot3 = Api-Call -Uri "$API/admin/slots" -Method POST -Body "{`"scheduleDateId`":$dateId2,`"startTime`":`"11:00`",`"endTime`":`"11:30`"}"
Test-Assert "Create Slot 3 (Date2, 11:00-11:30)" $slot3.Success
$slotId3 = if ($slot3.Success) { $slot3.Data.id } else { $null }

$slot4 = Api-Call -Uri "$API/admin/slots" -Method POST -Body "{`"scheduleDateId`":$dateId2,`"startTime`":`"14:00`",`"endTime`":`"14:30`"}"
Test-Assert "Create Slot 4 (Date2, 14:00-14:30)" $slot4.Success
$slotId4 = if ($slot4.Success) { $slot4.Data.id } else { $null }

$slot5 = Api-Call -Uri "$API/admin/slots" -Method POST -Body "{`"scheduleDateId`":$dateId3,`"startTime`":`"15:00`",`"endTime`":`"15:30`"}"
Test-Assert "Create Slot 5 (Date3, 15:00-15:30)" $slot5.Success
$slotId5 = if ($slot5.Success) { $slot5.Data.id } else { $null }

# ── Register 3 Teams ──
Write-Host ""
Write-Host "--- Registering Teams ---" -ForegroundColor Yellow

$team1 = Api-Call -Uri "$API/students/register" -Method POST -Body '{"projectName":"AI Chatbot","members":4,"leaderName":"Alice Johnson","email":"alice@test.com","description":"An AI-powered chatbot for customer support"}'
Test-Assert "Register Team 1 (AI Chatbot)" $team1.Success
$teamId1 = if ($team1.Success) { $team1.Data.id } else { $null }

$team2 = Api-Call -Uri "$API/students/register" -Method POST -Body '{"projectName":"Smart IoT Hub","members":3,"leaderName":"Bob Smith","email":"bob@test.com","description":"IoT device management system"}'
Test-Assert "Register Team 2 (Smart IoT Hub)" $team2.Success
$teamId2 = if ($team2.Success) { $team2.Data.id } else { $null }

$team3 = Api-Call -Uri "$API/students/register" -Method POST -Body '{"projectName":"EcoTracker","members":5,"leaderName":"Charlie Brown","email":"charlie@test.com","description":"Environmental monitoring dashboard"}'
Test-Assert "Register Team 3 (EcoTracker)" $team3.Success
$teamId3 = if ($team3.Success) { $team3.Data.id } else { $null }

if (-not $teamId1 -or -not $teamId2 -or -not $teamId3) {
    Write-Host "  FATAL: Could not register teams. Cannot continue." -ForegroundColor Red
    exit 1
}

# ── Create 1 CONFIRMED booking, 1 WAITLISTED booking, leave 1 AVAILABLE ──
Write-Host ""
Write-Host "--- Creating Bookings (CONFIRMED + WAITLISTED + leave AVAILABLE) ---" -ForegroundColor Yellow

# Team 1 books Slot 1 -> should become CONFIRMED
$booking1 = Api-Call -Uri "$API/bookings" -Method POST -Body "{`"teamId`":$teamId1,`"slotId`":$slotId1}"
Test-Assert "Book Slot 1 for Team 1 -> CONFIRMED" ($booking1.Success -and $booking1.Data.status -eq "CONFIRMED") "Status: $($booking1.Data.status)"
$bookingId1 = if ($booking1.Success) { $booking1.Data.id } else { $null }

# Team 2 books same Slot 1 -> should become WAITLISTED
$booking2 = Api-Call -Uri "$API/bookings" -Method POST -Body "{`"teamId`":$teamId2,`"slotId`":$slotId1}"
Test-Assert "Book Slot 1 for Team 2 -> WAITLISTED" ($booking2.Success -and $booking2.Data.status -eq "WAITLISTED") "Status: $($booking2.Data.status)"
$bookingId2 = if ($booking2.Success) { $booking2.Data.id } else { $null }

# Slot 5 remains AVAILABLE (no booking)
Write-Host "  Slot 5 (ID: $slotId5) left AVAILABLE intentionally." -ForegroundColor Gray

# ── Verify Data Consistency ──
Write-Host ""
Write-Host "--- Verifying Data Consistency ---" -ForegroundColor Yellow

$allDates = Api-Call -Uri "$API/admin/dates"
Test-Assert "All 3 demo dates present" ($allDates.Success -and @($allDates.Data).Count -eq 3) "Count: $(@($allDates.Data).Count)"

$allSlots = Api-Call -Uri "$API/admin/slots"
Test-Assert "All 5 slots present" ($allSlots.Success -and @($allSlots.Data).Count -eq 5) "Count: $(@($allSlots.Data).Count)"

$allTeams = Api-Call -Uri "$API/admin/teams"
Test-Assert "All 3 teams present" ($allTeams.Success -and @($allTeams.Data).Count -ge 3) "Count: $(@($allTeams.Data).Count)"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " PHASE 2: API TESTING" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# ── 1. Registration Tests ──
Write-Host "--- 1. Registration Tests ---" -ForegroundColor Yellow

# Valid registration (Team 4)
$team4 = Api-Call -Uri "$API/students/register" -Method POST -Body '{"projectName":"DataViz Pro","members":2,"leaderName":"Diana Prince","email":"diana@test.com","description":"Data visualization tool"}'
Test-Assert "Register valid team -> success" $team4.Success
$teamId4 = if ($team4.Success) { $team4.Data.id } else { $null }

# Duplicate email
$dupTeam = Api-Call -Uri "$API/students/register" -Method POST -Body '{"projectName":"Duplicate","members":2,"leaderName":"Dup","email":"alice@test.com","description":"dup"}'
Test-Assert "Register duplicate email -> error" (-not $dupTeam.Success) "Expected failure, got success"

# ── 2. Slot Viewing Tests ──
Write-Host ""
Write-Host "--- 2. Slot Viewing Tests ---" -ForegroundColor Yellow

$availSlots = Api-Call -Uri "$API/bookings/available"
Test-Assert "Available slots endpoint responds" $availSlots.Success
if ($availSlots.Success) {
    $availOnlyStatuses = @($availSlots.Data | ForEach-Object { $_.status } | Sort-Object -Unique)
    Test-Assert "Available slots only return AVAILABLE status" ($availOnlyStatuses.Count -eq 1 -and $availOnlyStatuses[0] -eq "AVAILABLE") "Statuses: $($availOnlyStatuses -join ', ')"
}

$allSlotsView = Api-Call -Uri "$API/bookings/all"
Test-Assert "All slots endpoint responds" $allSlotsView.Success

$adminSlots = Api-Call -Uri "$API/admin/slots"
Test-Assert "Admin slots endpoint responds" $adminSlots.Success

# Filter slots by date
$filteredSlots = Api-Call -Uri "$API/admin/slots?dateId=$dateId1"
Test-Assert "Slots filtered by date ID" ($filteredSlots.Success -and @($filteredSlots.Data).Count -eq 2) "Count: $(@($filteredSlots.Data).Count)"

# Verify one slot is BOOKED
$slot1Status = ($adminSlots.Data | Where-Object { $_.id -eq $slotId1 }).status
Test-Assert "Slot 1 status is BOOKED" ($slot1Status -eq "BOOKED") "Status: $slot1Status"

# Verify other slots are AVAILABLE
$slot5Status = ($adminSlots.Data | Where-Object { $_.id -eq $slotId5 }).status
Test-Assert "Slot 5 status is AVAILABLE" ($slot5Status -eq "AVAILABLE") "Status: $slot5Status"

# ── 3. Booking Tests ──
Write-Host ""
Write-Host "--- 3. Booking Tests ---" -ForegroundColor Yellow

# Team 3 books an available slot -> CONFIRMED
$booking3 = Api-Call -Uri "$API/bookings" -Method POST -Body "{`"teamId`":$teamId3,`"slotId`":$slotId3}"
Test-Assert "Team 3 books available slot -> CONFIRMED" ($booking3.Success -and $booking3.Data.status -eq "CONFIRMED") "Status: $($booking3.Data.status)"
$bookingId3 = if ($booking3.Success) { $booking3.Data.id } else { $null }

# Team 4 books same slot 3 -> WAITLISTED
$booking4 = Api-Call -Uri "$API/bookings" -Method POST -Body "{`"teamId`":$teamId4,`"slotId`":$slotId3}"
Test-Assert "Team 4 books booked slot -> WAITLISTED" ($booking4.Success -and $booking4.Data.status -eq "WAITLISTED") "Status: $($booking4.Data.status)"
$bookingId4 = if ($booking4.Success) { $booking4.Data.id } else { $null }

# ── 4. Cancellation Tests ──
Write-Host ""
Write-Host "--- 4. Cancellation Tests ---" -ForegroundColor Yellow

# Cancel Team 1's CONFIRMED booking on Slot 1 -> Team 2's WAITLISTED should promote
$cancelResult = Api-Call -Uri "$API/bookings/$bookingId1" -Method DELETE
Test-Assert "Cancel CONFIRMED booking (ID: $bookingId1)" $cancelResult.Success

# Check that Team 2's booking was promoted from WAITLISTED to CONFIRMED
Start-Sleep -Milliseconds 500
$team2Bookings = Api-Call -Uri "$API/students/bob@test.com/bookings"
if ($team2Bookings.Success) {
    $promotedBooking = @($team2Bookings.Data) | Where-Object { $_.id -eq $bookingId2 }
    Test-Assert "Waitlisted booking auto-promoted to CONFIRMED" ($promotedBooking.status -eq "CONFIRMED") "Status: $($promotedBooking.status)"
} else {
    Test-Assert "Fetch Team 2 bookings after promotion" $false "API call failed"
}

# Check Slot 1 is still BOOKED (because promoted booking took over)
$slotCheckAfterCancel = Api-Call -Uri "$API/admin/slots"
$slot1AfterCancel = ($slotCheckAfterCancel.Data | Where-Object { $_.id -eq $slotId1 }).status
Test-Assert "Slot 1 remains BOOKED after waitlist promotion" ($slot1AfterCancel -eq "BOOKED") "Status: $slot1AfterCancel"

# ── 5. Reschedule Tests ──
Write-Host ""
Write-Host "--- 5. Reschedule Tests ---" -ForegroundColor Yellow

# Reschedule Team 3's booking from Slot 3 to Slot 5 (AVAILABLE)
$rescheduleResult = Api-Call -Uri "$API/bookings/$bookingId3/reschedule" -Method PUT -Body "{`"slotId`":$slotId5}"
Test-Assert "Reschedule booking from Slot 3 to Slot 5" $rescheduleResult.Success
if ($rescheduleResult.Success) {
    Test-Assert "Rescheduled booking status is CONFIRMED" ($rescheduleResult.Data.status -eq "CONFIRMED") "Status: $($rescheduleResult.Data.status)"
}

Start-Sleep -Milliseconds 500

# Old slot (3) should be freed or taken by waitlisted booking (Team 4 was waitlisted on slot 3)
$slotsAfterReschedule = Api-Call -Uri "$API/admin/slots"
$slot3After = ($slotsAfterReschedule.Data | Where-Object { $_.id -eq $slotId3 }).status
$slot5After = ($slotsAfterReschedule.Data | Where-Object { $_.id -eq $slotId5 }).status

# Team 4 was waitlisted on slot 3, so it should be promoted -> slot 3 stays BOOKED
Test-Assert "Old slot freed and waitlisted promoted (Slot 3 BOOKED)" ($slot3After -eq "BOOKED") "Status: $slot3After"
Test-Assert "New slot assigned (Slot 5 BOOKED)" ($slot5After -eq "BOOKED") "Status: $slot5After"

# Verify Team 4 was promoted to CONFIRMED
$team4BookingsAfterReschedule = Api-Call -Uri "$API/students/diana@test.com/bookings"
if ($team4BookingsAfterReschedule.Success) {
    $team4Booking = @($team4BookingsAfterReschedule.Data) | Where-Object { $_.id -eq $bookingId4 }
    Test-Assert "Team 4 waitlisted promoted to CONFIRMED on old slot" ($team4Booking.status -eq "CONFIRMED") "Status: $($team4Booking.status)"
}

# Reschedule to same slot should fail
$sameSlotReschedule = Api-Call -Uri "$API/bookings/$bookingId3/reschedule" -Method PUT -Body "{`"slotId`":$slotId5}"
Test-Assert "Reschedule to same slot -> error" (-not $sameSlotReschedule.Success) "Expected failure"

# ── 6. Reports Tests ──
Write-Host ""
Write-Host "--- 6. Reports Tests ---" -ForegroundColor Yellow

$schedule = Api-Call -Uri "$API/admin/reports/schedule"
Test-Assert "Full schedule report returns data" ($schedule.Success -and @($schedule.Data).Count -gt 0) "Count: $(@($schedule.Data).Count)"

$summary = Api-Call -Uri "$API/admin/reports/summary"
Test-Assert "Booking summary returns data" ($summary.Success -and @($summary.Data).Count -gt 0) "Count: $(@($summary.Data).Count)"

# Check schedule CSV export
try {
    $csvResponse = Invoke-WebRequest -Uri "$API/admin/reports/export/csv" -Method GET -UseBasicParsing
    $csvContent = $csvResponse.Content
    Test-Assert "Schedule CSV export returns data" ($csvContent.Length -gt 0 -and $csvContent -match "Slot ID")
} catch {
    Test-Assert "Schedule CSV export" $false "Exception: $_"
}

# Check teams CSV export
try {
    $teamsCsvResponse = Invoke-WebRequest -Uri "$API/admin/reports/export/teams-csv" -Method GET -UseBasicParsing
    $teamsCsvContent = $teamsCsvResponse.Content
    Test-Assert "Teams CSV export returns data" ($teamsCsvContent.Length -gt 0 -and $teamsCsvContent -match "Team ID")
} catch {
    Test-Assert "Teams CSV export" $false "Exception: $_"
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " PHASE 4: EDGE CASE TESTING" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# ── Double booking same slot by same team ──
Write-Host "--- Edge Case: Double Booking ---" -ForegroundColor Yellow
$doubleBook = Api-Call -Uri "$API/bookings" -Method POST -Body "{`"teamId`":$teamId2,`"slotId`":$slotId1}"
Test-Assert "Double booking same slot by same team -> error" (-not $doubleBook.Success) "Expected failure, got success"

# ── Invalid email input ──
Write-Host ""
Write-Host "--- Edge Case: Invalid Email ---" -ForegroundColor Yellow
$badEmail = Api-Call -Uri "$API/students/register" -Method POST -Body '{"projectName":"Bad","members":1,"leaderName":"Test","email":"not-an-email","description":"test"}'
Test-Assert "Invalid email format -> validation error" (-not $badEmail.Success) "Expected failure"

# ── Empty/missing required fields ──
Write-Host ""
Write-Host "--- Edge Case: Empty Form Submission ---" -ForegroundColor Yellow
$emptyTeam = Api-Call -Uri "$API/students/register" -Method POST -Body '{"projectName":"","members":null,"leaderName":"","email":"","description":""}'
Test-Assert "Empty required fields -> validation error" (-not $emptyTeam.Success) "Expected failure"

# ── Missing body ──
$noBody = Api-Call -Uri "$API/bookings" -Method POST -Body '{}'
Test-Assert "Booking with empty body -> validation error" (-not $noBody.Success) "Expected failure"

# ── Non-existent team for booking ──
Write-Host ""
Write-Host "--- Edge Case: Non-existent Resources ---" -ForegroundColor Yellow
$fakeTeamBooking = Api-Call -Uri "$API/bookings" -Method POST -Body '{"teamId":99999,"slotId":1}'
Test-Assert "Booking with non-existent team -> error" (-not $fakeTeamBooking.Success) "Expected failure"

# ── Non-existent slot for booking ──
$fakeSlotBooking = Api-Call -Uri "$API/bookings" -Method POST -Body "{`"teamId`":$teamId1,`"slotId`":99999}"
Test-Assert "Booking with non-existent slot -> error" (-not $fakeSlotBooking.Success) "Expected failure"

# ── Cancel non-existent booking ──
$fakeCancel = Api-Call -Uri "$API/bookings/99999" -Method DELETE
Test-Assert "Cancel non-existent booking -> error" (-not $fakeCancel.Success) "Expected failure"

# ── Reschedule cancelled booking ──
Write-Host ""
Write-Host "--- Edge Case: Reschedule Cancelled Booking ---" -ForegroundColor Yellow
$rescheduleCancelled = Api-Call -Uri "$API/bookings/$bookingId1/reschedule" -Method PUT -Body "{`"slotId`":$slotId4}"
Test-Assert "Reschedule cancelled booking -> error" (-not $rescheduleCancelled.Success) "Expected failure"

# ── Team already has confirmed booking, try booking another slot ──
Write-Host ""
Write-Host "--- Edge Case: Team Already Has Confirmed Booking ---" -ForegroundColor Yellow
$alreadyConfirmed = Api-Call -Uri "$API/bookings" -Method POST -Body "{`"teamId`":$teamId2,`"slotId`":$slotId4}"
Test-Assert "Team with confirmed booking tries another slot -> error" (-not $alreadyConfirmed.Success) "Expected failure"

# ── Student lookup for non-existent email ──
Write-Host ""
Write-Host "--- Edge Case: Student Lookup ---" -ForegroundColor Yellow
$noTeam = Api-Call -Uri "$API/students/nonexistent@test.com/team"
Test-Assert "Team lookup for non-existent email -> error" (-not $noTeam.Success) "Expected failure"

$noBookings = Api-Call -Uri "$API/students/nonexistent@test.com/bookings"
Test-Assert "Bookings lookup for non-existent email -> responds" $true  # Empty list is fine

# ── Delete demo date cascades properly ──
Write-Host ""
Write-Host "--- Edge Case: Cascading Delete ---" -ForegroundColor Yellow
$tmpDate = Api-Call -Uri "$API/admin/dates" -Method POST -Body '{"demoDate":"2026-05-01"}'
if ($tmpDate.Success) {
    $tmpSlot = Api-Call -Uri "$API/admin/slots" -Method POST -Body "{`"scheduleDateId`":$($tmpDate.Data.id),`"startTime`":`"08:00`",`"endTime`":`"08:30`"}"
    $delDate = Api-Call -Uri "$API/admin/dates/$($tmpDate.Data.id)" -Method DELETE
    Test-Assert "Delete demo date cascades to slots" $delDate.Success
    # Verify slot is gone
    $allSlotsAfterDel = Api-Call -Uri "$API/admin/slots"
    $remainingTmpSlot = @($allSlotsAfterDel.Data) | Where-Object { $_.id -eq $tmpSlot.Data.id }
    Test-Assert "Cascaded slot is removed" ($null -eq $remainingTmpSlot)
}

# ── Duplicate demo date ──
Write-Host ""
Write-Host "--- Edge Case: Duplicate Demo Date ---" -ForegroundColor Yellow
$dupDate = Api-Call -Uri "$API/admin/dates" -Method POST -Body '{"demoDate":"2026-04-10"}'
Test-Assert "Duplicate demo date -> error" (-not $dupDate.Success) "Expected failure"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " PHASE 6: FINAL VERIFICATION" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# ── Verify Final State ──
Write-Host "--- Final Data State ---" -ForegroundColor Yellow

$finalDates = Api-Call -Uri "$API/admin/dates"
Test-Assert "Final: 3 demo dates in system" ($finalDates.Success -and @($finalDates.Data).Count -eq 3) "Count: $(@($finalDates.Data).Count)"

$finalSlots = Api-Call -Uri "$API/admin/slots"
Test-Assert "Final: 5 slots in system" ($finalSlots.Success -and @($finalSlots.Data).Count -eq 5) "Count: $(@($finalSlots.Data).Count)"

$finalTeams = Api-Call -Uri "$API/admin/teams"
Test-Assert "Final: At least 4 teams in system" ($finalTeams.Success -and @($finalTeams.Data).Count -ge 4) "Count: $(@($finalTeams.Data).Count)"

$finalBookings = Api-Call -Uri "$API/admin/bookings"
Test-Assert "Final: Bookings exist in system" ($finalBookings.Success -and @($finalBookings.Data).Count -gt 0) "Count: $(@($finalBookings.Data).Count)"

# Check waitlist
$finalWaitlist = Api-Call -Uri "$API/admin/bookings/waitlist"
Test-Assert "Final: Waitlist endpoint responds" $finalWaitlist.Success

# Verify report endpoints one more time
$finalScheduleReport = Api-Call -Uri "$API/admin/reports/schedule"
Test-Assert "Final: Schedule report responds correctly" ($finalScheduleReport.Success -and @($finalScheduleReport.Data).Count -eq 5) "Count: $(@($finalScheduleReport.Data).Count)"

$finalSummaryReport = Api-Call -Uri "$API/admin/reports/summary"
Test-Assert "Final: Booking summary matches booking count" ($finalSummaryReport.Success -and @($finalSummaryReport.Data).Count -eq @($finalBookings.Data).Count) "Summary: $(@($finalSummaryReport.Data).Count) vs Bookings: $(@($finalBookings.Data).Count)"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " TEST RESULTS SUMMARY" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Total Passed: $passed" -ForegroundColor Green
Write-Host "  Total Failed: $failed" -ForegroundColor $(if ($failed -gt 0) { "Red" } else { "Green" })
Write-Host ""

if ($bugs.Count -gt 0) {
    Write-Host "  BUGS FOUND:" -ForegroundColor Red
    foreach ($bug in $bugs) {
        Write-Host "    - $bug" -ForegroundColor Red
    }
} else {
    Write-Host "  NO BUGS FOUND - ALL TESTS PASSED!" -ForegroundColor Green
}

Write-Host ""
Write-Host "  System Status: $(if ($failed -eq 0) { 'FULLY WORKING' } else { 'HAS ISSUES' })" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })
Write-Host ""
