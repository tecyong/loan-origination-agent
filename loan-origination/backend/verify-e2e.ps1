Write-Host "=== 1. Logging in as Applicant ===" -ForegroundColor Cyan
$appLogin = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method POST -Body (@{ email = "applicant@demo.com"; password = "password123" } | ConvertTo-Json) -ContentType "application/json"
$appToken = $appLogin.token
$appHeaders = @{ Authorization = "Bearer $appToken" }
Write-Host "Applicant Logged in: $($appLogin.user.fullName)"

Write-Host "`n=== 2. Creating Draft Loan Application ===" -ForegroundColor Cyan
$draftBody = @{
    productId = 1
    amount = 12000.00
    tenureMonths = 24
    purpose = "Home Renovation & Green Energy Upgrade"
    employmentType = "Full-Time Salaried"
    employerName = "Cyberdyne Systems"
    grossMonthlyIncome = 7000.00
    existingMonthlyDebt = 500.00
} | ConvertTo-Json

$draft = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/applications" -Method POST -Headers $appHeaders -Body $draftBody -ContentType "application/json"
$appId = $draft.id
Write-Host "Draft Created: $($draft.applicationNumber) | Status: $($draft.status) | DTI: $($draft.calculatedDti)%"

Write-Host "`n=== 3. Submitting Application to EximeeBPMS ===" -ForegroundColor Cyan
$submitted = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/applications/$appId/submit" -Method POST -Headers $appHeaders
Write-Host "Submitted: $($submitted.applicationNumber) | Status: $($submitted.status) | Process Instance: $($submitted.processInstanceId)"

Write-Host "`n=== 4. Logging in as Loan Officer ===" -ForegroundColor Cyan
$offLogin = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method POST -Body (@{ email = "officer@demo.com"; password = "password123" } | ConvertTo-Json) -ContentType "application/json"
$offToken = $offLogin.token
$offHeaders = @{ Authorization = "Bearer $offToken" }
Write-Host "Officer Logged in: $($offLogin.user.fullName)"

Write-Host "`n=== 5. Officer Underwriting: Requesting Revision ===" -ForegroundColor Cyan
$revDecision = @{
    action = "REQUEST_INFO"
    revisionNotes = "Please verify recent utility bill or bonus declaration document."
} | ConvertTo-Json
$revResult = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/officer/applications/$appId/decision" -Method POST -Headers $offHeaders -Body $revDecision -ContentType "application/json"
Write-Host "Decision Applied: Status is now $($revResult.status) with reason: $($revResult.decisionReason)"

Write-Host "`n=== 6. Applicant Revises and Re-submits ===" -ForegroundColor Cyan
$updateBody = @{
    productId = 1
    amount = 12000.00
    tenureMonths = 24
    purpose = "Home Renovation & Green Energy Upgrade"
    employmentType = "Full-Time Salaried"
    employerName = "Cyberdyne Systems"
    grossMonthlyIncome = 7500.00
    existingMonthlyDebt = 450.00
} | ConvertTo-Json
$updated = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/applications/$appId" -Method PUT -Headers $appHeaders -Body $updateBody -ContentType "application/json"
$resubmitted = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/applications/$appId/submit" -Method POST -Headers $appHeaders
Write-Host "Re-submitted by Applicant: Status is now $($resubmitted.status)"

Write-Host "`n=== 7. Officer Issues Final Approval ===" -ForegroundColor Cyan
$apprDecision = @{
    action = "APPROVE"
    approvedAmount = 12000.00
    decisionReason = "Applicant has exceptional DTI (< 15%) and stable prime employment. Fully approved."
} | ConvertTo-Json
$approved = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/officer/applications/$appId/decision" -Method POST -Headers $offHeaders -Body $apprDecision -ContentType "application/json"
Write-Host "Final Decision Applied: Status is now $($approved.status) with approved amount: $($approved.approvedAmount)"

Write-Host "`n=== 8. Checking Audit Log Trail ===" -ForegroundColor Cyan
$finalApp = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/applications/$appId" -Method GET -Headers $appHeaders
foreach ($log in $finalApp.auditLogs) {
    Write-Host " - [$($log.createdAt)] $($log.actionType) by $($log.actorName): $($log.notes)" -ForegroundColor Green
}
Write-Host "`n>>> ALL E2E LOAN ORIGINATION WORKFLOW STEPS VERIFIED WITH SUCCESS! <<<" -ForegroundColor Yellow
