import XCTest
final class SalatiUITests:XCTestCase {
    func testChooseOfflineCityAndNavigate() {
        let app=XCUIApplication();app.launchArguments=["-uiTestingReset"];app.launch()
        app.buttons["chooseLocation"].tap()
        let search=app.searchFields.firstMatch
        XCTAssertTrue(search.waitForExistence(timeout:10));search.tap();search.typeText("Damascus")
        let city=app.buttons.matching(NSPredicate(format:"identifier BEGINSWITH 'city.'")).firstMatch
        XCTAssertTrue(city.waitForExistence(timeout:10));city.tap()
        let proceed=app.buttons["continueOnboarding"]
        app.swipeUp();XCTAssertTrue(proceed.waitForExistence(timeout:5));proceed.tap()
        XCTAssertTrue(app.tabBars.buttons["Calendar"].waitForExistence(timeout:10))
        capture("Prayer home")
        app.tabBars.buttons["Calendar"].tap();capture("Hijri calendar")
        app.tabBars.buttons["Qibla"].tap();capture("Qibla without simulator heading")
        app.tabBars.buttons["Settings"].tap();capture("Settings")
    }
    func capture(_ name:String) {let attachment=XCTAttachment(screenshot:XCUIApplication().screenshot());attachment.name=name;attachment.lifetime = .keepAlways;add(attachment)}
}
